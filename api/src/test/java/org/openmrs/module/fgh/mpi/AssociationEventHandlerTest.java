package org.openmrs.module.fgh.mpi;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.openmrs.module.debezium.DatabaseOperation.DELETE;

import java.util.Map;

import org.junit.Test;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseOperation;
import org.openmrs.module.fgh.mpi.handler.AssociationEventHandler;

public class AssociationEventHandlerTest {
	
	private AssociationEventHandler handler = new AssociationEventHandler();
	
	private DatabaseEvent createEvent(String table, DatabaseOperation op, Map prevState, Map newState) {
		return new DatabaseEvent(null, table, op, null, prevState, newState);
	}
	
	@Test
	public void getPatientId_shouldGetThePatientIdForAPersonNameEvent() {
		final Integer personId = 3;
		DatabaseEvent event = createEvent("person_name", null, null, singletonMap("person_id", personId));
		assertEquals(personId, handler.getPatientId(event));
	}
	
	@Test
	public void getPatientId_shouldGetThePatientIdForAPersonAttributeEvent() {
		final Integer personId = 3;
		DatabaseEvent event = createEvent("person_attribute", null, null, singletonMap("person_id", personId));
		assertEquals(personId, handler.getPatientId(event));
	}
	
	@Test
	public void getPatientId_shouldGetThePatientIdForAPersonAddressEvent() {
		final Integer personId = 3;
		DatabaseEvent event = createEvent("person_address", null, null, singletonMap("person_id", personId));
		assertEquals(personId, handler.getPatientId(event));
	}
	
	@Test
	public void getPatientId_shouldGetThePatientIdForAPatientIdentifierEvent() {
		final Integer patientId = 3;
		DatabaseEvent event = createEvent("patient_identifier", null, null, singletonMap("patient_id", patientId));
		assertEquals(patientId, handler.getPatientId(event));
	}
	
	@Test
	public void getPatientId_shouldGetThePatientIdForAnEncounterEvent() {
		final Integer patientId = 3;
		DatabaseEvent event = createEvent("encounter", null, null, singletonMap("patient_id", patientId));
		assertEquals(patientId, handler.getPatientId(event));
	}
	
	@Test
	public void getPatientId_shouldGetThePatientIdForADeleteEvent() {
		final Integer patientId = 3;
		DatabaseEvent event = createEvent("encounter", DELETE, singletonMap("patient_id", patientId), null);
		assertEquals(patientId, handler.getPatientId(event));
	}
	
}
