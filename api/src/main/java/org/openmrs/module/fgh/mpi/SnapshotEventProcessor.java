package org.openmrs.module.fgh.mpi;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.openmrs.api.APIException;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes snapshot events in parallel
 */
public class SnapshotEventProcessor extends BaseEventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(SnapshotEventProcessor.class);
	
	private ExecutorService executor;
	
	private List<CompletableFuture<Map<String, Object>>> futures;
	
	private AtomicInteger successCount;
	
	private Long start;
	
	private Integer lastSubmittedPatientId;
	
	private int threadCount;
	
	public SnapshotEventProcessor(int threadCount) {
		super(true);
		this.threadCount = threadCount;
		executor = Executors.newFixedThreadPool(threadCount);
		futures = synchronizedList(new ArrayList(threadCount));
		successCount = new AtomicInteger();
		start = null;
		lastSubmittedPatientId = MpiUtils.getLastSubmittedPatientId();
	}
	
	@Override
	public void process(DatabaseEvent event) {
		if (start == null) {
			start = currentTimeMillis();
			log.info("Patient full sync started at: " + new Date());
		}
		
		boolean isLastPatient = event.getSnapshot() == DatabaseEvent.Snapshot.LAST;
		Integer curPatientId = Integer.valueOf(event.getPrimaryKeyId().toString());
		if (lastSubmittedPatientId != null && lastSubmittedPatientId >= curPatientId) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping patient with id: " + curPatientId + " that was already submitted to the MPI");
			}
			
			//TODO test this logic in a deployed env
			if (isLastPatient) {
				MpiUtils.deletePatientIdOffsetFile();
			}
			
			return;
		}
		
		futures.add(CompletableFuture.supplyAsync(() -> {
			try {
				Thread.currentThread().setName(event.getTableName() + "-" + event.getPrimaryKeyId());
				log.info("Processing database event -> " + event);
				final long startSingle = System.currentTimeMillis();
				
				Map<String, Object> fhirPatient = createFhirResource(event);
				
				log.info("Done generating fhir patient for database event -> " + event);
				
				if (log.isDebugEnabled()) {
					log.debug("Duration: " + (currentTimeMillis() - startSingle) + "ms");
				}
				
				return fhirPatient;
			}
			catch (Throwable e) {
				log.error("An error occurred while processing event -> " + event);
				throw new APIException(e);
				//TODO We should record the failed patient details and generate a report so that when we have a way to
				// sync a single patient, we sync only the failed patients instead of all
			}
			
		}, executor));
		
		if (futures.size() == threadCount || isLastPatient) {
			try {
				if (log.isDebugEnabled()) {
					log.debug("Waiting for " + futures.size() + " event processor thread(s) to terminate");
				}
				
				CompletableFuture<Void> allFuture = CompletableFuture
				        .allOf(futures.toArray(new CompletableFuture[futures.size()]));
				
				allFuture.get();
				
				if (log.isDebugEnabled()) {
					log.debug("Processor event thread(s) terminated");
				}
				
				List<Map<String, Object>> fhirPatients = Collections.synchronizedList(new ArrayList(futures.size()));
				futures.parallelStream().forEach(p -> {
					if (p != null) {
						try {
							Map<String, Object> returnedPatient = p.get();
							if (returnedPatient != null) {
								fhirPatients.add(Collections.singletonMap(MpiConstants.FIELD_RESOURCE, returnedPatient));
							}
						}
						catch (Exception e) {
							throw new APIException("Failed to get patient resource from future", e);
						}
					}
				});
				
				if (!fhirPatients.isEmpty()) {
					Map<String, Object> fhirBundle = new HashMap(3);
					fhirBundle.put(MpiConstants.FIELD_RESOURCE_TYPE, MpiConstants.BUNDLE);
					//fhirBundle.put(MpiConstants.FIELD_TYPE, MpiConstants.BATCH);
					fhirBundle.put(MpiConstants.FIELD_TYPE, "message");
					fhirBundle.put(MpiConstants.FIELD_ENTRY, fhirPatients);
					
					List<Object> response = mpiHttpClient.submitBundle(mapper.writeValueAsString(fhirBundle));
					//The response is a list of 2 MPI identifiers for each patient
					int successPatientCount = response.size() / 2;
					if (fhirPatients.size() == successPatientCount) {
						if (log.isDebugEnabled()) {
							log.debug("All patients in the batch were successfully processed by the MPI");
						}
						
						successCount.addAndGet(fhirPatients.size());
						
						MpiUtils.saveLastSubmittedPatientId(Integer.valueOf(event.getPrimaryKeyId().toString()));
					} else {
						//TODO Loop through all patients in the batch and check which records were problematic
						throw new APIException((fhirPatients.size() - successPatientCount)
						        + " patient(s) in the batch were not successfully processed by the MPI");
					}
				}
			}
			catch (Exception e) {
				//TODO We should record the failed patients in this batch and generate a report
				//throw new APIException("An error occurred while processing patient batch", e);
				
				log.debug("An error ocurred...");
			}
			finally {
				futures.clear();
			}
			
			if (isLastPatient) {
				log.info("============================= Statistics =============================");
				log.info("Patients submitted: " + successCount.get());
				log.info("Started at        : " + new Date(start));
				log.info("Ended at          : " + new Date());
				
				long duration = currentTimeMillis() - start;
				
				log.info("Duration          : " + DurationFormatUtils.formatDuration(duration, "HH:mm:ss", true));
				log.info("======================================================================");
				
				try {
					MpiUtils.deletePatientIdOffsetFile();
				}
				finally {
					log.info("Switching to incremental loading");
					
					Utils.updateGlobalProperty(MpiConstants.GP_INITIAL, "false");
				}
			}
		}
	}
	
}
