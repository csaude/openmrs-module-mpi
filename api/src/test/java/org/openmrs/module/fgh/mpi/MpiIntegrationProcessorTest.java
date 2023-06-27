package org.openmrs.module.fgh.mpi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.openmrs.module.debezium.DatabaseOperation.CREATE;
import static org.openmrs.module.debezium.DatabaseOperation.DELETE;
import static org.openmrs.module.debezium.DatabaseOperation.READ;
import static org.openmrs.module.debezium.DatabaseOperation.UPDATE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ACTIVE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CONTACT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_RELATIONSHIP;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_AUTHENTICATION_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_APP_CONTENT_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_SECRET;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_EVENT_URI;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.PATIENT_QUERY;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.PERSON_QUERY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.fgh.mpi.miscellaneous.AuthenticationType;
import org.openmrs.module.fgh.mpi.miscellaneous.MpiContext;
import org.openmrs.module.fgh.mpi.miscellaneous.MpiSystemType;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MpiUtils.class, FhirUtils.class, Context.class, MpiContext.class, ApplicationContext.class,
        PatientAndPersonEventHandler.class })
public class MpiIntegrationProcessorTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private Logger mockLogger;
	
	private MpiIntegrationProcessor processor = new MpiIntegrationProcessor();
	
	@Mock
	private SnapshotEventProcessor snapshotEventProcessor;
	
	private ApplicationContext context;
	
	@Mock
	private AdministrationService adminService;
	
	private static final String UUID_SYSTEM = "http://test.openmrs.id/uuid";
	
	private static final String MESSAGE_HEADER_REFERENCE = "metadata.epts.e-saude.net/bundle";
	
	private static final String MESSAGE_HEADER_EVENT_URI = "urn:ihe:iti:pmir:2019:patient-feed";
	
	private static final AuthenticationType AUTHENTICATION_TYPE = AuthenticationType.OAUTH;
	
	private static final String MPI_BASE_URL = "http://sante.org.mz";
	
	private static final String MPI_APP_CONTENT_TYPE = "application/fhir+json";
	
	private static final MpiSystemType MPI_SYSTEM = MpiSystemType.SANTEMPI;
	
	private static final MpiSystemType MPI_SYSTEM_AS_OPENCR = MpiSystemType.SANTEMPI;
	
	private static final String SANTE_CLIENT_ID = "client_credentials";
	
	private static final String SANTE_CLIENT_SECRET = "bG6TuS3X-H1MsT4ctW!CxXjK9J4l1QpK8B0Q";
	
	private MpiContext mpiContext = new MpiContext();
	
	@Before
	public void setup() throws Exception {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(FhirUtils.class);
		PowerMockito.mockStatic(MpiContext.class);
		PowerMockito.mockStatic(ApplicationContext.class);
		Whitebox.setInternalState(MpiIntegrationProcessor.class, Logger.class, mockLogger);
		Whitebox.setInternalState(PatientAndPersonEventHandler.class, Logger.class, mockLogger);
		Whitebox.setInternalState(processor, MpiHttpClient.class, mockMpiHttpClient);
		when(Context.getAdministrationService()).thenReturn(adminService);
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(AUTHENTICATION_TYPE.toString());
		when(adminService.getGlobalProperty(GP_MPI_BASE_URL)).thenReturn(MPI_BASE_URL);
		when(adminService.getGlobalProperty(GP_MPI_SYSTEM)).thenReturn(MPI_SYSTEM.toString());
		when(adminService.getGlobalProperty(GP_MPI_APP_CONTENT_TYPE)).thenReturn(MPI_APP_CONTENT_TYPE);
		when(adminService.getGlobalProperty(GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE)).thenReturn(MESSAGE_HEADER_REFERENCE);
		when(adminService.getGlobalProperty(GP_SANTE_MESSAGE_HEADER_EVENT_URI)).thenReturn(MESSAGE_HEADER_EVENT_URI);
		when(adminService.getGlobalProperty(GP_SANTE_MESSAGE_HEADER_EVENT_URI)).thenReturn(MESSAGE_HEADER_EVENT_URI);
		when(adminService.getGlobalProperty(GP_SANTE_CLIENT_ID)).thenReturn(SANTE_CLIENT_ID);
		when(adminService.getGlobalProperty(GP_SANTE_CLIENT_SECRET)).thenReturn(SANTE_CLIENT_SECRET);
		when(MpiUtils.getGlobalPropertyValue(GP_UUID_SYSTEM)).thenReturn(UUID_SYSTEM);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		mpiContext.setAuthenticationType(AUTHENTICATION_TYPE);
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
	public void process_shouldIgnoreAPersonDeleteEventIfTsnapshotEventProcessorheTheMpiPatientRecordIsInactive()
	        throws Exception {
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
		Map newPatient = singletonMap(FIELD_CONTACT,
		    asList(newContact1, singletonMap(FIELD_ID, "relationship-uuid-3"), newContact2));
		when(FhirUtils.buildPatient(anyString(), anyBoolean(), anyList(), anyMap())).thenReturn(newPatient);
		
		processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null));
		
		Mockito.verify(mockMpiHttpClient).submitPatient(Matchers.argThat(new ArgumentMatcher<String>() {
			
			@Override
			public boolean matches(Object json) {
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
		Map newPatient = singletonMap(FIELD_CONTACT, asList(newContact1, null));
		when(FhirUtils.buildPatient(anyString(), anyBoolean(), anyList(), anyMap())).thenReturn(newPatient);
		
		processor.process(1, new DatabaseEvent(null, "patient", UPDATE, null, null, null));
		
		Mockito.verify(mockMpiHttpClient).submitPatient(Matchers.argThat(new ArgumentMatcher<String>() {
			
			@Override
			public boolean matches(Object json) {
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
			}
		}));
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
	public void process_shouldInitTheContextWithoutSSL() throws Exception {
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(AUTHENTICATION_TYPE.toString());
		Mockito.verify(snapshotEventProcessor, times(0))
		        .process(new DatabaseEvent(1234, "patient", UPDATE, null, null, null));
	}
	
	@Test
	public void process_shouldInitTheContextWithtSSL() throws Exception {
		AuthenticationType SSL = AuthenticationType.SSL;
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(SSL.toString());
		Mockito.verify(snapshotEventProcessor, times(0))
		        .process(new DatabaseEvent(12345, "patient", UPDATE, null, null, null));
	}
	
	@Test
	public void process_shouldgetPatientInRemoteMasterPatientIndexAsSanteMPI() throws Exception {
		when(adminService.getGlobalProperty(GP_MPI_SYSTEM)).thenReturn(MPI_SYSTEM.toString());
		final String patientUuid = "patient-uuid-for-sante";
		Map res = new HashMap();
		res.put("active", true);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(res);
		Map prevState = singletonMap("uuid", patientUuid);
		assertNotNull(processor.process(1, new DatabaseEvent(null, "person", DELETE, null, prevState, null)));
		Mockito.verify(mockMpiHttpClient, times(1)).getPatient(patientUuid);
	}
	
	@Test
	public void process_shouldgetPatientInRemoteMasterPatientIndexAsOpenCR() throws Exception {
		when(adminService.getGlobalProperty(GP_MPI_SYSTEM)).thenReturn(MPI_SYSTEM_AS_OPENCR.toString());
		final String patientUuid = "patient-uuid-for-openCR";
		Map res = new HashMap();
		res.put("active", true);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(res);
		Map prevState = singletonMap("uuid", patientUuid);
		assertNotNull(processor.process(1, new DatabaseEvent(null, "person", DELETE, null, prevState, null)));
		Mockito.verify(mockMpiHttpClient, times(1)).getPatient(patientUuid);
	}
	
	@Test
	public void shouldProcessBundleData() throws Exception {
		List<Object> response = new ArrayList<>();
		Map<String, Object> messageHeader = FhirUtils.generateMessageHeader();
		List<Map<String, Object>> entryList = new ArrayList<>();
		entryList.add(messageHeader);
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> resourceData = new HashMap<>();
		resourceData.put("resourceType", "Bundle");
		resourceData.put("resource", resourceData);
		data.put("entry", entryList);
		
		String bundleData = data.toString();
		when(mockMpiHttpClient.submitBundle("/fhiir-url/test", bundleData, List.class)).thenReturn(response);
		Mockito.verify(mockMpiHttpClient, times(0)).submitBundle("/fhir-url/test", bundleData, List.class);
		assertFalse(response == null);
		
	}
	
	@Test
	public void shouldRetrieveAccessTokenAndProcessBundleData() throws Exception {
		final String patientUuid = "patient-uuid";
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(singletonMap(FIELD_ACTIVE, false));
		
		List<Object> response = new ArrayList<>();
		Map<String, Object> messageHeader = FhirUtils.generateMessageHeader();
		List<Map<String, Object>> entryList = new ArrayList<>();
		entryList.add(messageHeader);
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> resourceData = new HashMap<>();
		resourceData.put("resourceType", "Bundle");
		resourceData.put("resource", resourceData);
		data.put("entry", entryList);
		
		String bundleData = data.toString();
		when(mockMpiHttpClient.submitBundle("/fhiir-url/test", bundleData, List.class)).thenReturn(response);
		assertFalse(response == null);
	}
	
	@Test
	public void should_processDataAndSubmitPatient() throws Exception {
		final String patientUuid = "patient-uuid";
		Mockito.verify(mockMpiHttpClient, times(0)).submitPatient(patientUuid);
		
		doAnswer(invocation -> {
			Object firstArgument = invocation.getArguments()[0];
			assertEquals(patientUuid, firstArgument);
			return null;
		}).when(mockMpiHttpClient).submitPatient(patientUuid);
		
	}
}
