/*
package org.openmrs.module.fgh.mpi;

import java.util.Collections;

import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.debezium.entity.DatabaseEvent;
import org.openmrs.module.debezium.entity.DatabaseEvent.Snapshot;
import org.openmrs.module.debezium.entity.DatabaseOperation;
import org.openmrs.module.fgh.mpi.listener.MpiDebeziumEngineConfig;

public class MpiDatabaseEventListenerTest {
	
	private MpiDebeziumEngineConfig config;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		config = new MpiDebeziumEngineConfig();
	}
	
	//@Test
	public void accept_shouldProcessAPatientInsertEvent() {
		DatabaseEvent event = new DatabaseEvent(1, "patient", DatabaseOperation.CREATE, Snapshot.FALSE, null,
		        Collections.singletonMap("uuid", "uuid-1"));
		
		//consumer.onEvent(event);
	}
	
}
*/
