package org.openmrs.module.fgh.mpi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.debezium.DatabaseOperation.CREATE;
import static org.openmrs.module.debezium.DatabaseOperation.DELETE;
import static org.openmrs.module.debezium.DatabaseOperation.READ;
import static org.openmrs.module.debezium.DatabaseOperation.UPDATE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ACTIVE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CONTACT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_NAME;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_RELATIONSHIP;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.PATIENT_QUERY;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.PERSON_QUERY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.module.debezium.DatabaseEvent;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MpiUtils.class, FhirUtils.class })
@PowerMockIgnore("javax.management.*")
public class MpiIntegrationProcessorTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private Logger mockLogger;
	
	private MpiIntegrationProcessor processor = new MpiIntegrationProcessor();
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(FhirUtils.class);
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
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
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
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
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
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
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
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
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
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
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
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		when(MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString()))).thenReturn(emptyList());
		
		res = processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null));
		
		assertEquals(false, res.get(FIELD_ACTIVE));
	}
	
	@Test
	public void process_shouldClearTheRelationTypeForContactsToUpdateInTheMpi() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(asList(asList(null, null, false, null, patientUuid, false)));
		when(MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(singletonList(singletonList(false)));
		
		final String relationshipUuid1 = "relationship-uuid-1";
		final String relationshipUuid2 = "relationship-uuid-2";
		Map mpiContact1 = new HashMap();
		mpiContact1.put(FIELD_ID, relationshipUuid1);
		mpiContact1.put(FIELD_RELATIONSHIP, emptyMap());
		Map mpiContact2 = new HashMap();
		mpiContact2.put(FIELD_ID, relationshipUuid2);
		mpiContact2.put(FIELD_RELATIONSHIP, emptyMap());
		Map mpiPatient = new HashMap();
		mpiPatient.put(FIELD_ACTIVE, true);
		List mpiContacts = asList(mpiContact1, singletonMap(FIELD_RELATIONSHIP, emptyMap()), mpiContact2);
		mpiPatient.put(FIELD_CONTACT, mpiContacts);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(mpiPatient);
		
		Map newContact1 = new HashMap();
		newContact1.put(FIELD_ID, relationshipUuid1);
		newContact1.put(FIELD_RELATIONSHIP, emptyMap());
		Map newContact2 = new HashMap();
		newContact2.put(FIELD_ID, relationshipUuid2);
		newContact2.put(FIELD_RELATIONSHIP, emptyMap());
		Map newPatient = new HashMap();
		newPatient.put(FIELD_CONTACT, asList(newContact1, singletonMap(FIELD_ID, "relationship-uuid-3"), newContact2));
		newPatient.put(FIELD_NAME, "patient-name");
		when(FhirUtils.buildPatient(anyString(), anyBoolean(), anyList(), anyMap())).thenReturn(newPatient);
		
		processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null));
		
		Mockito.verify(mockMpiHttpClient).submitPatient(ArgumentMatchers.argThat(json -> {
			try {
				Map updatedContact1 = new HashMap();
				updatedContact1.put(FIELD_ID, relationshipUuid1);
				updatedContact1.put(FIELD_RELATIONSHIP, null);
				Map updatedContact2 = new HashMap();
				updatedContact2.put(FIELD_ID, relationshipUuid2);
				updatedContact2.put(FIELD_RELATIONSHIP, null);
				Map expectedMpiPatient = new HashMap();
				expectedMpiPatient.put(FIELD_ACTIVE, true);
				expectedMpiPatient.put(FIELD_CONTACT,
				    asList(updatedContact1, singletonMap(FIELD_RELATIONSHIP, emptyMap()), updatedContact2));
				
				return expectedMpiPatient.equals(new ObjectMapper().readValue(json.toString(), Map.class));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}
	
	@Test
	public void process_shouldNotClearTheRelationTypeIfThereAreNoContactsToUpdateInTheMpi() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(asList(asList(null, null, false, null, patientUuid, false)));
		when(MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(singletonList(singletonList(false)));
		
		final String relationshipUuid1 = "relationship-uuid-1";
		final String relationshipUuid2 = "relationship-uuid-2";
		Map mpiContact1 = new HashMap();
		mpiContact1.put(FIELD_ID, relationshipUuid1);
		mpiContact1.put(FIELD_RELATIONSHIP, emptyMap());
		Map mpiContact2 = new HashMap();
		mpiContact2.put(FIELD_ID, relationshipUuid2);
		mpiContact2.put(FIELD_RELATIONSHIP, emptyMap());
		Map mpiPatient = new HashMap();
		mpiPatient.put(FIELD_ACTIVE, true);
		List mpiContacts = asList(mpiContact1, singletonMap(FIELD_RELATIONSHIP, emptyMap()), mpiContact2);
		mpiPatient.put(FIELD_CONTACT, mpiContacts);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(mpiPatient);
		Map newContact1 = new HashMap();
		newContact1.put(FIELD_ID, "relationship-uuid-3");
		newContact1.put(FIELD_RELATIONSHIP, emptyMap());
		Map newContact2 = new HashMap();
		newContact2.put(FIELD_ID, "relationship-uuid-4");
		newContact2.put(FIELD_RELATIONSHIP, emptyMap());
		Map newPatient = singletonMap(FIELD_CONTACT, asList(newContact1, newContact2));
		when(FhirUtils.buildPatient(anyString(), anyBoolean(), anyList(), anyMap())).thenReturn(newPatient);
		processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null));
		Mockito.verify(mockMpiHttpClient, Mockito.never()).submitPatient(anyString());
	}
	
	@Test
	public void process_shouldIgnoreContactsReplacedWithNull() throws Exception {
		final Integer patientId = 1;
		final String patientUuid = "patient-uuid";
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(asList(asList(null, null, false, null, patientUuid, false)));
		when(MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(singletonList(singletonList(false)));
		
		final String relationshipUuid1 = "relationship-uuid-1";
		final String relationshipUuid2 = "relationship-uuid-2";
		Map mpiContact1 = new HashMap();
		mpiContact1.put(FIELD_ID, relationshipUuid1);
		mpiContact1.put(FIELD_RELATIONSHIP, emptyMap());
		Map mpiContact2 = new HashMap();
		mpiContact2.put(FIELD_ID, relationshipUuid2);
		mpiContact2.put(FIELD_RELATIONSHIP, emptyMap());
		Map mpiPatient = new HashMap();
		mpiPatient.put(FIELD_ACTIVE, true);
		List mpiContacts = asList(mpiContact1, mpiContact2);
		mpiPatient.put(FIELD_CONTACT, mpiContacts);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(mpiPatient);
		
		Map newContact1 = new HashMap();
		newContact1.put(FIELD_ID, relationshipUuid1);
		newContact1.put(FIELD_RELATIONSHIP, emptyMap());
		Map newPatient = new HashMap();
		newPatient.put(FIELD_CONTACT, asList(newContact1, null));
		newPatient.put(FIELD_NAME, "patient-name");
		when(FhirUtils.buildPatient(anyString(), anyBoolean(), anyList(), anyMap())).thenReturn(newPatient);
		
		processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null));
		
		Mockito.verify(mockMpiHttpClient).submitPatient(ArgumentMatchers.argThat(json -> {
			try {
				Map updatedContact1 = new HashMap();
				updatedContact1.put(FIELD_ID, relationshipUuid1);
				updatedContact1.put(FIELD_RELATIONSHIP, null);
				Map updatedContact2 = new HashMap();
				updatedContact2.put(FIELD_ID, relationshipUuid2);
				updatedContact2.put(FIELD_RELATIONSHIP, emptyMap());
				Map expectedMpiPatient = new HashMap();
				expectedMpiPatient.put(FIELD_ACTIVE, true);
				expectedMpiPatient.put(FIELD_CONTACT, asList(updatedContact1, updatedContact2));
				
				return expectedMpiPatient.equals(new ObjectMapper().readValue(json.toString(), Map.class));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
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
	
	@Test
	public void process_shouldSkipPatientWithNoName() throws Exception {
		String patientId = "1";
		String patientUuid = "patient-uuid";
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(singletonMap(FIELD_ACTIVE, false));
		when(MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(singletonList(singletonList(false)));
		Map<String, Object> patientData = processor.process(1, new DatabaseEvent(null, null, null, null, null, null));
		assertNull(patientData);
	}
}
