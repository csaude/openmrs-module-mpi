package org.openmrs.module.fgh.mpi.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.entity.DatabaseEvent;
import org.openmrs.module.debezium.entity.DatabaseOperation;
import org.openmrs.module.debezium.utils.Utils;
import org.openmrs.module.fgh.mpi.entity.InitialLoadTaskController;
import org.openmrs.module.fgh.mpi.integ.MpiContext;
import org.openmrs.module.fgh.mpi.integ.MpiHttpClient;
import org.openmrs.module.fgh.mpi.utils.FhirUtils;
import org.openmrs.module.fgh.mpi.utils.MpiConstants;
import org.openmrs.module.fgh.mpi.utils.MpiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedList;
import static org.openmrs.module.fgh.mpi.processor.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.utils.FhirUtils.fastCreateMap;
import static org.openmrs.module.fgh.mpi.utils.FhirUtils.generateMessageHeader;
import static org.openmrs.module.fgh.mpi.utils.FhirUtils.getObjectInMapAsMap;
import static org.openmrs.module.fgh.mpi.utils.FhirUtils.getObjectOnMapAsListOfMap;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_INITIAL_BATCH_SIZE;

public class InitialLoadProcessor extends BaseEventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(InitialLoadProcessor.class);
	
	private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	
	private static final int BATCH_SIZE = 10000;
	
	private static final int MPI_BATCH_SIZE = Integer.parseInt(MpiUtils.getGlobalPropertyValue(GP_INITIAL_BATCH_SIZE));
	
	private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
	
	private final List<CompletableFuture<Map<String, Object>>> futures = synchronizedList(new ArrayList<>(THREAD_COUNT));
	
	private final AtomicInteger successCount = new AtomicInteger(0);
	
	private Long start;
	
	private List<Integer> lastSubmittedPatientIds = new ArrayList<>();
	
	private final PatientService patientService = Context.getPatientService();
	
	private final MpiHttpClient mpiHttpClient;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private InitialLoadTaskController initialLoadTaskController;
	
	public InitialLoadProcessor(MpiHttpClient mpiHttpClient) {
		super(true);
		this.mpiHttpClient = mpiHttpClient;
	}
	
	public void runInitialLoad() {
		try {
			this.initialLoadTaskController = MpiUtils.fetchInitialLoadTaskController();
			
			if (this.initialLoadTaskController != null && this.initialLoadTaskController.isRunning()
			        && this.initialLoadTaskController.getEndDate() != null) {
				log.info("Initial load task is already running");
				return;
			}
		}
		catch (SQLException e) {
			log.error("Error while fetching initial load task info", e);
			throw new RuntimeException(e);
		}
		
		try {
			MpiContext.initIfNecessary();
		}
		catch (Exception e) {
			throw new APIException("Failed to initialize MpiContext", e);
		}
		
		log.info("Starting Patient initial load process");
		start = System.currentTimeMillis();
		boolean continueProcessing = true;
		
		try {
			while (continueProcessing) {
				Integer lastId = this.initialLoadTaskController != null ? this.initialLoadTaskController.getPatientOffsetId()
				        : 0;
				Integer id = MpiUtils.getLastSubmittedPatientId() != null ? MpiUtils.getLastSubmittedPatientId() : lastId;
				
				List<Integer> patientsId = MpiUtils.executePatientQuery(
				    MpiIntegrationProcessor.PATIENT_QUERY_BOUNDARIES.replace(ID_PLACEHOLDER, String.valueOf(id))
				            .replace(MpiIntegrationProcessor.MAXIMUM_RESULT_PLACEHOLDER, String.valueOf(BATCH_SIZE)));
				
				log.info("Found {} Patients for initial load process", patientsId.size());
				if (patientsId.isEmpty()) {
					if (this.initialLoadTaskController != null) {
						this.initialLoadTaskController.setRunning(false);
						this.initialLoadTaskController.setEndDate(new Date());
						this.initialLoadTaskController.setActive(Boolean.FALSE);
						MpiUtils.updateInitialLoadTaskController(this.initialLoadTaskController);
					}
					continueProcessing = false;
					log.info("No patients found for initial load process");
				} else {
					
					//Create a task controller
					this.initialLoadTaskController = this.createLoadTaskController(initialLoadTaskController, lastId);
					List<List<Integer>> batches = partitionList(patientsId, MPI_BATCH_SIZE);
					
					for (List<Integer> batch : batches) {
						processBatch(batch);
					}
					
					//after all save the controller
					this.saveController(patientsId.get(patientsId.size() - 1));
					executor.shutdown();
					executor.awaitTermination(1, TimeUnit.HOURS);
				}
			}
		}
		catch (Exception e) {
			log.error("Execution shutdown interrupted", e);
			Integer lastProcessedId = MpiUtils.getLastSubmittedPatientId() != null ? MpiUtils.getLastSubmittedPatientId()
			        : 0;
			this.saveController(lastProcessedId);
			
			throw new APIException("An error occurred processing patient batch ", e);
		}
		
		// Finalize process
		logFinalizeStats();
	}
	
	private void saveController(Integer lastProcessedPatientId) {
		this.initialLoadTaskController.setPatientOffsetId(lastProcessedPatientId);
		MpiUtils.updateInitialLoadTaskController(initialLoadTaskController);
	}
	
	private InitialLoadTaskController createLoadTaskController(InitialLoadTaskController initialLoadTaskController,
	        Integer patientId) {
		if (initialLoadTaskController == null) {
			initialLoadTaskController = new InitialLoadTaskController();
			initialLoadTaskController.setStartDate(new Date());
			initialLoadTaskController.setEndDate(null);
			initialLoadTaskController.setActive(Boolean.TRUE);
			initialLoadTaskController.setRunning(Boolean.TRUE);
			initialLoadTaskController.setPatientOffsetId(patientId);
			MpiUtils.createInitialLoadTaskController(initialLoadTaskController);
		}
		
		return initialLoadTaskController;
	}
	
	private void processBatch(List<Integer> patients) {
		List<DatabaseEvent> events = patients.stream().map(
		    patientId -> this.convertPatientToDatabaseEvent(patientId, patients.get(patients.size() - 1).equals(patientId)))
		        .collect(Collectors.toList());
		
		for (DatabaseEvent event : events) {
			process(event);
		}
		
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		submitToMpi();
	}
	
	@Override
	public void process(DatabaseEvent event) {
		futures.add(CompletableFuture.supplyAsync(() -> {
			try {
				Thread.currentThread().setName(event.getTableName() + "-" + event.getPrimaryKeyId());
				log.info("Processing Patient {}", event.getPrimaryKeyId());
				long startSingle = System.currentTimeMillis();
				Map<String, Object> fhirPatient = createFhirResource(event);
				log.debug("Duration:  {}", (System.currentTimeMillis() - startSingle));
				lastSubmittedPatientIds.add((Integer) event.getPrimaryKeyId());
				
				return fhirPatient;
			}
			catch (Throwable e) {
				log.error("Error processing Patient {}", event.getPrimaryKeyId(), e);
				throw new APIException(e);
			}
		}, executor));
		
		if (futures.size() == MPI_BATCH_SIZE) {
			submitToMpi();
			lastSubmittedPatientIds.clear();
		}
		
	}
	
	private void submitToMpi() {
		MpiContext mpiContext;
		try {
			mpiContext = MpiContext.initIfNecessary();
		}
		catch (Exception e) {
			throw new APIException("Failed to init MpiContext", e);
		}
		
		try {
			
			List<Map<String, Object>> fhirPatients = futures.stream().map(f -> {
				try {
					return f.get();
				}
				catch (Exception e) {
					throw new APIException("Failed to get FHIR patient", e);
				}
			}).filter(Objects::nonNull).map(p -> {
				Map<String, Object> entry = new HashMap<>();
				entry.put(MpiConstants.FIELD_RESOURCE, p);
				return entry;
			}).collect(Collectors.toList());
			
			if (fhirPatients.isEmpty()) {
				log.info("The fhir payload of patient is empty du");
				
			} else if (mpiContext.getMpiSystem().isOpenCr()) {
				openCrImplementations(fhirPatients);
				
			} else if (mpiContext.getMpiSystem().isSanteMPI()) {
				Map<String, Object> messageBundle = santeMpiImplementation(fhirPatients);
				mpiHttpClient.submitBundle("fhir/Bundle", objectMapper.writeValueAsString(messageBundle), Map.class);
				
			} else {
				throw new APIException("Unknown MPISystem [" + mpiContext.getMpiSystem() + "]");
			}
			successCount.addAndGet(fhirPatients.size());
		}
		catch (Exception e) {
			throw new APIException("MPI SUBMISSION FAILED", e);
		}
		finally {
			if (!lastSubmittedPatientIds.isEmpty()) {
				MpiUtils.saveLastSubmittedPatientId(lastSubmittedPatientIds.get(lastSubmittedPatientIds.size() - 1));
			}
			futures.clear();
		}
	}
	
	private void openCrImplementations(List<Map<String, Object>> fhirPatients) throws Exception {
		Map<String, Object> bundle = new HashMap<>();
		bundle.put(MpiConstants.FIELD_RESOURCE_TYPE, MpiConstants.BUNDLE);
		bundle.put(MpiConstants.FIELD_TYPE, MpiConstants.BATCH);
		bundle.put(MpiConstants.FIELD_ENTRY, fhirPatients);
		
		List<?> response = mpiHttpClient.submitBundle("fhir", objectMapper.writeValueAsString(bundle), List.class);
		int successPatientCount = response.size() / 2;
		if (fhirPatients.size() != successPatientCount) {
			throw new APIException((fhirPatients.size() - successPatientCount) + " patients failed");
		}
	}
	
	private Map<String, Object> santeMpiImplementation(List<Map<String, Object>> fhirPatients) {
		Map<String, Object> fhirMessageHeaderEntry = generateMessageHeader();
		
		//The entry of resource in message bundle message
		Map<String, Object> fhirResourceEntry = new HashMap<>(2);
		
		fhirResourceEntry.put("fullUrl", FhirUtils.santeMessageHeaderFocusReference);
		fhirResourceEntry.put("resource", new HashMap<>(3));
		
		getObjectInMapAsMap("resource", fhirResourceEntry).put(MpiConstants.FIELD_RESOURCE_TYPE, MpiConstants.BUNDLE);
		
		getObjectInMapAsMap("resource", fhirResourceEntry).put(MpiConstants.FIELD_TYPE, MpiConstants.FIELD_TYPE_HISTORY);
		
		List<Map<String, Object>> fhirPatientsPlusRequest = new ArrayList<Map<String, Object>>(fhirPatients.size());
		
		for (Map<String, Object> fhirPatient : fhirPatients) {
			Map<String, Object> resourceEntry = getObjectInMapAsMap(MpiConstants.FIELD_RESOURCE, fhirPatient);
			
			Object patientUuid = getObjectOnMapAsListOfMap(MpiConstants.FIELD_IDENTIFIER, resourceEntry).get(0).get("value");
			
			Map<String, Object> patientEntry = fastCreateMap(MpiConstants.FIELD_RESOURCE, resourceEntry, "request",
			    fastCreateMap("method", "POST", "url", "Patient/" + patientUuid), "fullUrl", "Patient/" + patientUuid);
			
			fhirPatientsPlusRequest.add(patientEntry);
		}
		
		getObjectInMapAsMap("resource", fhirResourceEntry).put(MpiConstants.FIELD_ENTRY, fhirPatientsPlusRequest);
		
		Map<String, Object> messageBundle = new HashMap<String, Object>();
		messageBundle.put("resourceType", "Bundle");
		messageBundle.put("type", "message");
		messageBundle.put(MpiConstants.FIELD_ENTRY, new ArrayList<Map<String, Object>>(2));
		
		getObjectOnMapAsListOfMap(MpiConstants.FIELD_ENTRY, messageBundle).add(fhirMessageHeaderEntry);
		getObjectOnMapAsListOfMap(MpiConstants.FIELD_ENTRY, messageBundle).add(fhirResourceEntry);
		
		return messageBundle;
	}
	
	private DatabaseEvent convertPatientToDatabaseEvent(Integer patientId, boolean isLastBatch) {
		return new DatabaseEvent(patientId, "patient", DatabaseOperation.READ,
		        isLastBatch ? DatabaseEvent.Snapshot.LAST : DatabaseEvent.Snapshot.FALSE, null, null);
	}
	
	private void logFinalizeStats() {
		long duration = System.currentTimeMillis() - start;
		log.info("Patients submitted: " + successCount.get());
		log.info("Duration: " + DurationFormatUtils.formatDuration(duration, "HH:mm:ss", true));
		log.info("============================= Statistics =============================");
		log.info("Patients submitted: " + successCount.get());
		log.info("Started at        : " + new Date(start));
		log.info("Ended at          : " + new Date());
		log.info("Duration          : "
		        + org.apache.commons.lang3.time.DurationFormatUtils.formatDuration(duration, "HH:mm:ss", true));
		log.info("======================================================================");
		log.info("Switching to incremental loading");
		
		MpiUtils.deletePatientIdOffsetFile();
		Utils.updateGlobalProperty(MpiConstants.GP_INITIAL, "false");
	}
	
	private <T> List<List<T>> partitionList(List<T> list, int size) {
		List<List<T>> partitions = new ArrayList<>();
		for (int i = 0; i < list.size(); i += size) {
			partitions.add(list.subList(i, Math.min(i + size, list.size())));
		}
		return partitions;
	}
}
