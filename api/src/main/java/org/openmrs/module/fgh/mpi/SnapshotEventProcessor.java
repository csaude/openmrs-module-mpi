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
		
		try {
			log.info("Processing database event -> " + event);
			
			final long start = System.currentTimeMillis();
			
			Map<String, Object> fhirResource = createFhirResource(event);
			if (fhirResource != null) {
				//Because a relationship references 2 persons, process all
				if (MpiConstants.BUNDLE.equals(fhirResource.get((MpiConstants.FIELD_RESOURCE_TYPE)))) {
					for (Map<String, Object> fhirPatient : (List<Map>) fhirResource.get(MpiConstants.FIELD_ENTRY)) {
						mpiHttpClient.submitPatient(mapper.writeValueAsString(fhirPatient));
					}
				} else {
					mpiHttpClient.submitPatient(mapper.writeValueAsString(fhirResource));
				}
			}
			
			log.info("Done processing database event -> " + event);
			
			if (log.isDebugEnabled()) {
				log.debug("Duration: " + (currentTimeMillis() - start) + "ms");
			}
		}
		catch (Throwable t) {
			log.error("An error occurred while processing event -> " + event, t);
			//throw new APIException(t);
		}
		
		MpiUtils.saveLastSubmittedPatientId(Integer.valueOf(event.getPrimaryKeyId().toString()));
		
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
