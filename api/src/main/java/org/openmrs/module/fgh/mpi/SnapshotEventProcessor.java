package org.openmrs.module.fgh.mpi;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.synchronizedList;
import static org.openmrs.module.fgh.mpi.MpiConstants.DEFAULT_THREAD_COUNT;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes snapshot events in parallel
 */
public class SnapshotEventProcessor implements EventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(SnapshotEventProcessor.class);
	
	private PatientAndPersonEventHandler patientHandler;
	
	//TODO make this configurable
	private final ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
	
	private final List<CompletableFuture<Void>> futures = synchronizedList(new ArrayList(DEFAULT_THREAD_COUNT));
	
	private static Long start;
	
	private AtomicInteger successCount = new AtomicInteger();
	
	private AtomicInteger failureCount = new AtomicInteger();
	
	public SnapshotEventProcessor(PatientAndPersonEventHandler patientHandler) {
		this.patientHandler = patientHandler;
		log.info("Snapshot started at: " + new Date());
	}
	
	@Override
	public void process(DatabaseEvent event) {
		if (start == null) {
			start = currentTimeMillis();
		}
		
		boolean isLastPatient = event.getSnapshot() == DatabaseEvent.Snapshot.LAST;
		
		futures.add(CompletableFuture.runAsync(() -> {
			
			try {
				Thread.currentThread().setName(event.getTableName() + "-" + event.getPrimaryKeyId());
				ProcessorUtils.processEvent(event, patientHandler, null);
				successCount.getAndIncrement();
			}
			catch (Throwable e) {
				failureCount.getAndIncrement();
				//TODO We should record the failed patient details and generated a report so that when we have a way to
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
			}
			catch (Exception e) {
				log.error("An error occurred while waiting for event processor thread(s) to terminate", e);
			}
			finally {
				futures.clear();
			}
			
			if (isLastPatient) {
				log.info("============================= Statistics =============================");
				log.info("Success Count    : " + successCount.get());
				log.info("Failure Count    : " + failureCount.get());
				log.info("Snapshot ended at: " + new Date());
				log.info("Duration         : " + ((currentTimeMillis() - start) / 1000) + "s");
				log.info("======================================================================");
			}
		}
	}
	
}
