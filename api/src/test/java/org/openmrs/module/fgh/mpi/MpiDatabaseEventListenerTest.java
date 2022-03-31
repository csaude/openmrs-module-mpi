package org.openmrs.module.fgh.mpi;

import java.util.Collections;

import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseEvent.Snapshot;
import org.openmrs.module.debezium.DatabaseOperation;

public class MpiDatabaseEventListenerTest {
	
	private MpiDatabaseEventListener listener;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		listener = new MpiDatabaseEventListener();
	}
	
	//@Test
	public void accept_shouldProcessAPatientInsertEvent() {
		DatabaseEvent event = new DatabaseEvent(1, "patient", DatabaseOperation.CREATE, Snapshot.FALSE, null,
		        Collections.singletonMap("uuid", "uuid-1"));
		
		//consumer.onEvent(event);
	}
	
}
