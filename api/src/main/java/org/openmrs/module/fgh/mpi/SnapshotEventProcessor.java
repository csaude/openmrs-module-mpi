package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.FhirUtils.fastCreateMap;
import static org.openmrs.module.fgh.mpi.FhirUtils.generateMessageHeader;
import static org.openmrs.module.fgh.mpi.FhirUtils.getObjectInMapAsMap;
import static org.openmrs.module.fgh.mpi.FhirUtils.getObjectOnMapAsListOfMap;
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
import org.apache.kafka.common.errors.ApiException;
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
		MpiContext mpiContext = null;
		
		try {
			mpiContext = MpiContext.initIfNecessary();
		}
		catch (Exception e) {
			throw new ApiException(e);
		}
		
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
					int successPatientCount = 0;
					
					if (mpiContext.getMpiSystem().isOpenCr()) {
						Map<String, Object> fhirBundle = new HashMap(3);
						fhirBundle.put(MpiConstants.FIELD_RESOURCE_TYPE, MpiConstants.BUNDLE);
						fhirBundle.put(MpiConstants.FIELD_TYPE, MpiConstants.BATCH);
						fhirBundle.put(MpiConstants.FIELD_ENTRY, fhirPatients);
						
						List<Object> response = mpiHttpClient.submitBundle("fhir", mapper.writeValueAsString(fhirBundle),
						    List.class);
						
						successPatientCount = response.size() / 2;
						
						if (fhirPatients.size() == successPatientCount) {
							if (log.isDebugEnabled()) {
								log.debug("All patients in the batch were successfully processed by the MPI");
							}
							
							successCount.addAndGet(fhirPatients.size());
						} else {
							//TODO Loop through all patients in the batch and check which records were problematic
							throw new APIException((fhirPatients.size() - successPatientCount)
							        + " patient(s) in the batch were not successfully processed by the MPI");
						}
						
					} else if (mpiContext.getMpiSystem().isSanteMPI()) {
						Map<String, Object> fhirMessageHeaderEntry = generateMessageHeader();
						
						//The entry of resource in message bundle message
						Map<String, Object> fhirResourceEntry = new HashMap<>(2);
						
						fhirResourceEntry.put("fullUrl", FhirUtils.santeMessageHeaderFocusReference);
						fhirResourceEntry.put("resource", new HashMap<>(3));
						
						getObjectInMapAsMap("resource", fhirResourceEntry).put(MpiConstants.FIELD_RESOURCE_TYPE,
						    MpiConstants.BUNDLE);
						
						getObjectInMapAsMap("resource", fhirResourceEntry).put(MpiConstants.FIELD_TYPE,
						    MpiConstants.FIELD_TYPE_HISTORY);
						
						List<Map<String, Object>> fhirPatintsPlusRequest = new ArrayList<Map<String, Object>>(
						        fhirPatients.size());
						
						for (Map<String, Object> fhirPatient : fhirPatients) {
							Map<String, Object> resourceEntry = getObjectInMapAsMap(MpiConstants.FIELD_RESOURCE,
							    fhirPatient);
							
							Object patientUuid = getObjectOnMapAsListOfMap(MpiConstants.FIELD_IDENTIFIER, resourceEntry)
							        .get(0).get("value");
							
							Map<String, Object> patientEntry = fastCreateMap(MpiConstants.FIELD_RESOURCE, resourceEntry,
							    "request", fastCreateMap("method", "POST", "url", "Parient/" + patientUuid), "fullUrl",
							    "Parient/" + patientUuid);
							
							fhirPatintsPlusRequest.add(patientEntry);
						}
						
						getObjectInMapAsMap("resource", fhirResourceEntry).put(MpiConstants.FIELD_ENTRY,
						    fhirPatintsPlusRequest);
						
						Map<String, Object> messageBundle = new HashMap<String, Object>();
						messageBundle.put("resourceType", "Bundle");
						messageBundle.put("type", "message");
						messageBundle.put(MpiConstants.FIELD_ENTRY, new ArrayList<Map<String, Object>>(2));
						
						getObjectOnMapAsListOfMap(MpiConstants.FIELD_ENTRY, messageBundle).add(fhirMessageHeaderEntry);
						getObjectOnMapAsListOfMap(MpiConstants.FIELD_ENTRY, messageBundle).add(fhirResourceEntry);
						
						mpiHttpClient.submitBundle("fhir/Bundle", mapper.writeValueAsString(messageBundle), Map.class);
					} else {
						throw new APIException("Unkown MPISystem [" + mpiContext.getMpiSystem() + "]");
					}
					MpiUtils.saveLastSubmittedPatientId(Integer.valueOf(event.getPrimaryKeyId().toString()));
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
