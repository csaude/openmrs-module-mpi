package org.openmrs.module.fgh.mpi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.debezium.DatabaseOperation.CREATE;
import static org.openmrs.module.debezium.DatabaseOperation.DELETE;
import static org.openmrs.module.debezium.DatabaseOperation.READ;
import static org.openmrs.module.debezium.DatabaseOperation.UPDATE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ACTIVE;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.PATIENT_QUERY;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.PERSON_QUERY;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class MpiIntegrationProcessorTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private Logger mockLogger;
	
	@Mock
	private AdministrationService mockAdminService;
	
	private MpiIntegrationProcessor processor = new MpiIntegrationProcessor();
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		when(Context.getAdministrationService()).thenReturn(mockAdminService);
		Whitebox.setInternalState(MpiIntegrationProcessor.class, Logger.class, mockLogger);
		Whitebox.setInternalState(processor, MpiHttpClient.class, mockMpiHttpClient);
	}
	
	@Test
	public void process_shouldIgnoreAPersonInsertEvent() throws Exception {
		assertNull(processor.process(null, new DatabaseEvent(null, "person", CREATE, null, null, null)));
		verify(mockLogger).info("Ignoring person insert event");
	}
	
	@Test
	public void process_shouldIgnoreAPersonUpdateEventIfThePersonRowNoLongerExists() throws Exception {
		final Integer personId = 1;
		assertNull(processor.process(personId, new DatabaseEvent(null, "person", UPDATE, null, null, null)));
		verify(mockLogger).info("Ignoring event because no person was found with id: " + personId);
	}
	
	@Test
	public void process_shouldIgnoreAPersonReadEventIfThePersonRowNoLongerExists() throws Exception {
		final Integer personId = 1;
		assertNull(processor.process(personId, new DatabaseEvent(null, "person", READ, null, null, null)));
		verify(mockLogger).info("Ignoring event because no person was found with id: " + personId);
	}
	
	@Test
	public void process_shouldIgnoreAPersonDeleteEventIfThePatientDoesNotExistInTheMpi() throws Exception {
		final String patientUuid = "patient-uuid";
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(null);
		Map prevState = singletonMap("uuid", patientUuid);
		assertNull(processor.process(1, new DatabaseEvent(null, "person", DELETE, null, prevState, null)));
		verify(mockLogger).info("Ignoring event because there is no record in the MPI to update for deleted person");
	}
	
	@Test
	public void process_shouldIgnoreAPatientDeleteEventIfThePatientDoesNotExistInTheMpi() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(null);
		when(mockAdminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString()), true))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		Map prevState = singletonMap("uuid", patientUuid);
		assertNull(processor.process(patientId, new DatabaseEvent(null, "patient", DELETE, null, prevState, null)));
		verify(mockLogger).info("Ignoring event because there is no record in the MPI to update for deleted patient");
	}
	
	@Test
	public void process_shouldIgnoreAPersonDeleteEventIfTheTheMpiPatientRecordIsInactive() throws Exception {
		final String patientUuid = "patient-uuid";
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(singletonMap(FIELD_ACTIVE, false));
		Map prevState = singletonMap("uuid", patientUuid);
		assertNull(processor.process(1, new DatabaseEvent(null, "person", DELETE, null, prevState, null)));
		verify(mockLogger)
		        .info("Ignoring event because the record in the MPI is already marked as inactive for deleted person");
	}
	
	@Test
	public void process_shouldIgnoreAPatientDeleteEventIfTheMpiPatientRecordIsInactive() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(singletonMap(FIELD_ACTIVE, false));
		when(mockAdminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString()), true))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		Map prevState = singletonMap("uuid", patientUuid);
		assertNull(processor.process(patientId, new DatabaseEvent(null, "patient", DELETE, null, prevState, null)));
		verify(mockLogger)
		        .info("Ignoring event because the record in the MPI is already marked as inactive for deleted patient");
	}
	
	@Test
	public void process_shouldMarkTheMpiPatientAsInactiveIfThePersonIsDeletedInOpenmrs() throws Exception {
		final String patientUuid = "patient-uuid";
		Map mpiPatient = new HashMap();
		mpiPatient.put(FIELD_ACTIVE, true);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(mpiPatient);
		Map prevState = singletonMap("uuid", patientUuid);
		
		Map fhirPatient = processor.process(1, new DatabaseEvent(null, "person", DELETE, null, prevState, null));
		
		assertEquals(false, fhirPatient.get(FIELD_ACTIVE));
	}
	
	@Test
	public void process_shouldMarkTheMpiPatientAsInactiveIfThePatientIsDeletedInOpenmrs() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		Map mpiPatient = new HashMap();
		mpiPatient.put(FIELD_ACTIVE, true);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(mpiPatient);
		when(mockAdminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString()), true))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		Map prevState = singletonMap("uuid", patientUuid);
		
		Map fhirPatient = processor.process(1, new DatabaseEvent(null, "patient", DELETE, null, prevState, null));
		
		assertEquals(false, fhirPatient.get(FIELD_ACTIVE));
	}
	
	@Test
	public void process_shouldReturnNullIfThePatientDoesNotExistInTheMpiAndOpenmrs() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(null);
		when(mockAdminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString()), true))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		
		assertNull(processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null)));
		
		verify(mockLogger).info("Ignoring event because there is no patient record both in OpenMRS and MPI");
		verify(mockLogger).info("Ignoring event because there is no record in the MPI to update");
	}
	
	@Test
	public void process_shouldReturnNullIfThePatientDoesNotExistInOpenmrsAndIsInActiveInTheMpi() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(singletonMap(FIELD_ACTIVE, false));
		when(mockAdminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString()), true))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		
		assertNull(processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null)));
		
		verify(mockLogger).info("Ignoring event because there is no patient record both in OpenMRS and MPI");
		verify(mockLogger).info("Ignoring event because the record in the MPI is already marked as inactive");
	}
	
	@Test
	public void process_shouldMarkTheMpiPatientAsInactiveIfThePatientDoesNotExistInOpenmrs() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		Map res = new HashMap();
		res.put(FIELD_ACTIVE, true);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(res);
		when(mockAdminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString()), true))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		when(mockAdminService.executeSQL(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString()), true))
		        .thenReturn(emptyList());
		
		res = processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null));
		
		assertEquals(false, res.get(FIELD_ACTIVE));
	}
	
	@Test
	public void process_shouldProcessAPatientThatDoesNotExistInTheMpi() throws Exception {
		processor.process(1, new DatabaseEvent(null, null, null, null, null, null));
	}
	
	@Test
	public void process_shouldProcessAPatientThatAlreadyExistsInTheMpi() throws Exception {
	}
	
	@Test
	public void process_shouldProcessAPersonDeleteEvent() throws Exception {
	}
	
	@Test
	public void process_shouldProcessAPatientDeleteEvent() throws Exception {
	}
	
}
