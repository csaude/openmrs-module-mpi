package org.openmrs.module.fgh.mpi;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.synchronizedList;
import static org.openmrs.module.fgh.mpi.MpiConstants.DEFAULT_THREAD_COUNT;

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

import org.openmrs.api.APIException;
import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes snapshot events in parallel
 */
public class SnapshotEventProcessor extends BaseEventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(SnapshotEventProcessor.class);
	
	//TODO make this configurable
	private final ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
	
	private final List<CompletableFuture<Map<String, Object>>> futures = synchronizedList(
	    new ArrayList(DEFAULT_THREAD_COUNT));
	
	private AtomicInteger successCount = new AtomicInteger();
	
	private static Long start;
	
	public SnapshotEventProcessor(PatientAndPersonEventHandler patientHandler, MpiHttpClient mpiHttpClient) {
		super(patientHandler, mpiHttpClient);
	}
	
	@Override
	public void process(DatabaseEvent event) {
		if (start == null) {
			start = currentTimeMillis();
			log.info("Patient full sync started at: " + new Date());
		}
		
		boolean isLastPatient = event.getSnapshot() == DatabaseEvent.Snapshot.LAST;
		
		futures.add(CompletableFuture.supplyAsync(() -> {
			try {
				Thread.currentThread().setName(event.getTableName() + "-" + event.getPrimaryKeyId());
				log.info("Processing database event -> " + event);
				final long startSingle = System.currentTimeMillis();
				
				Map<String, Object> fhirPatient = ProcessorUtils.createFhirResource(event, patientHandler, null);
				
				log.info("Done processing database event -> " + event);
				
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
		
		if (futures.size() == DEFAULT_THREAD_COUNT || isLastPatient) {
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
					fhirBundle.put(MpiConstants.FIELD_TYPE, MpiConstants.BATCH);
					fhirBundle.put(MpiConstants.FIELD_ENTRY, fhirPatients);
					
					List<Object> response = mpiHttpClient.submitBundle(mapper.writeValueAsString(fhirBundle));
					//The response is a list of 2 MPI identifiers for each patient
					int successPatientCount = response.size() / 2;
					if (fhirPatients.size() == successPatientCount) {
						if (log.isDebugEnabled()) {
							log.debug("All patients in the batch were successfully processed by the MPI");
						}
						
						successCount.addAndGet(fhirPatients.size());
					} else {
						//TODO Loop through all patients in the batch and check which records were problematic
						throw new APIException((fhirPatients.size() - successPatientCount)
						        + " patients in the batch were not successfully processed by the MPI");
					}
				}
			}
			catch (Exception e) {
				//TODO We should record the failed patients in this batch and generate a report
				throw new APIException("An error occurred while processing patient batch", e);
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
				long denominator;
				String units;
				if (duration < 60000) {
					denominator = 1000;
					units = "sec";
				} else if (duration < 3600000) {
					denominator = 60000;
					units = "min";
				} else {
					denominator = 3600000;
					units = "hrs";
				}
				
				log.info("Duration          : " + (duration / denominator) + units);
				log.info("======================================================================");
			}
		}
	}
	
}
