package org.openmrs.module.fgh.mpi.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.entity.DatabaseEvent;
import org.openmrs.module.debezium.entity.DatabaseOperation;
import org.openmrs.module.debezium.entity.DebeziumEventQueue;
import org.openmrs.module.debezium.mysql.MySqlSnapshotMode;
import org.openmrs.module.debezium.mysql.SnapshotMode;
import org.openmrs.module.debezium.service.DebeziumEventQueueService;
import org.openmrs.module.fgh.mpi.integ.MpiHttpClient;
import org.openmrs.module.fgh.mpi.processor.BaseEventProcessor;
import org.openmrs.module.fgh.mpi.processor.IncrementalEventProcessor;
import org.openmrs.module.fgh.mpi.processor.InitialLoadProcessor;
import org.openmrs.module.fgh.mpi.utils.MpiConstants;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MpiIntegrationTask extends AbstractTask {
	
	private static final Logger log = LoggerFactory.getLogger(MpiIntegrationTask.class);
	
	private final static String APPLICATION_NAME = "MPI";
	
	private BaseEventProcessor eventProcessor;
	
	public static Map<String, DatabaseOperation> DATABASE_OPERATIONS;
	
	public static Map<String, DatabaseEvent.Snapshot> SNAPSHOT;
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	static {
		DATABASE_OPERATIONS = new HashMap<>();
		DATABASE_OPERATIONS.put("C", DatabaseOperation.CREATE);
		DATABASE_OPERATIONS.put("D", DatabaseOperation.DELETE);
		DATABASE_OPERATIONS.put("r", DatabaseOperation.READ);
		DATABASE_OPERATIONS.put("U", DatabaseOperation.UPDATE);
		
		SNAPSHOT = new HashMap<>();
		SNAPSHOT.put("FALSE", DatabaseEvent.Snapshot.FALSE);
		SNAPSHOT.put("TRUE", DatabaseEvent.Snapshot.TRUE);
		SNAPSHOT.put("LAST", DatabaseEvent.Snapshot.LAST);
	}
	
	@Override
	public void execute() {
		if (!isExecuting) {
			
			DebeziumEventQueueService eventQueueService = Context.getService(DebeziumEventQueueService.class);
			log.info("Executing Mpi Integration Task");
			
			if (getSnapshotMode() == MySqlSnapshotMode.INITIAL) {
				log.info("Mpi set for initial load");
				
				MpiHttpClient mpiHttpClient = new MpiHttpClient();
				InitialLoadProcessor initialLoadProcessor = new InitialLoadProcessor(mpiHttpClient);
				initialLoadProcessor.runInitialLoad();
			} else {
				
				boolean keepFetching = true;
				eventProcessor = new IncrementalEventProcessor();
				
				while (keepFetching) {
					log.info("Mpi set for incremental load");
					Set<DebeziumEventQueue> eventQueueSet = eventQueueService.getApplicationEvents(APPLICATION_NAME);
					eventQueueSet.forEach((eventQueue) -> {
						eventProcessor.process(this.convertEventQueueToDatabaseEvent(eventQueue));
					});
					eventQueueService.commitEventQueue(APPLICATION_NAME);
					
					if (eventQueueSet.isEmpty()) {
						keepFetching = false;
					}
				}
				
				log.info("Finalized the incremental load");
			}
			
		} else {
			log.warn("Mpi Integration task is running");
		}
	}
	
	public SnapshotMode getSnapshotMode() {
		String initial = Context.getAdministrationService().getGlobalProperty(MpiConstants.GP_INITIAL);
		return Boolean.parseBoolean(initial) ? MySqlSnapshotMode.INITIAL : MySqlSnapshotMode.SCHEMA_ONLY;
	}
	
	public DatabaseEvent convertEventQueueToDatabaseEvent(DebeziumEventQueue eventQueue) {
		try {
			return new DatabaseEvent(eventQueue.getPrimaryKeyId(), eventQueue.getTableName(),
			        DATABASE_OPERATIONS.get(eventQueue.getOperation()),
			        SNAPSHOT.get(String.valueOf(eventQueue.getSnapshot())),
			        objectMapper.readValue(eventQueue.getPreviousState(), new TypeReference<HashMap<String, Object>>() {}),
			        objectMapper.readValue(eventQueue.getNewState(), new TypeReference<HashMap<String, Object>>() {}));
		}
		catch (Exception e) {
			log.error("Error converting event queue to DB event", e);
			throw new RuntimeException("Error:  " + e);
		}
	}
}
