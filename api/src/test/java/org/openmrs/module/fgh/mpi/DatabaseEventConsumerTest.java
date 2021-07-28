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

public class DatabaseEventConsumerTest {
	
	@Mock
	private MpiIntegrationProcessor mockProcessor;
	
	@Mock
	private PatientEventHandler mockPatientHandler;
	
	private DatabaseEventConsumer consumer;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		consumer = new DatabaseEventConsumer();
		setInternalState(consumer, "patientHandler", mockPatientHandler);
		setInternalState(consumer, "processor", mockProcessor);
	}
	
	@Test
	public void accept_shouldProcessAPatientInsertEvent() {
		DatabaseEvent event = new DatabaseEvent(1, "patient", DatabaseOperation.CREATE, Snapshot.FALSE, null,
		        Collections.singletonMap("uuid", "uuid-1"));
		
		consumer.accept(event);
	}
	
}
