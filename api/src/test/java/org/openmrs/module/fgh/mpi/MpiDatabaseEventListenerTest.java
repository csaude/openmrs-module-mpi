package org.openmrs.module.fgh.mpi;

import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseEvent.Snapshot;
import org.openmrs.module.debezium.DatabaseOperation;

public class MpiDatabaseEventListenerTest {
	
	@Mock
	private PatientAndPersonEventHandler mockHandler;
	
	private MpiDatabaseEventListener listener;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		listener = new MpiDatabaseEventListener();
		setInternalState(listener, "patientHandler", mockHandler);
	}
	
	@Test
	public void accept_shouldProcessAPatientInsertEvent() {
		DatabaseEvent event = new DatabaseEvent(1, "patient", DatabaseOperation.CREATE, Snapshot.FALSE, null,
		        Collections.singletonMap("uuid", "uuid-1"));
		
		//consumer.onEvent(event);
	}
	
}
