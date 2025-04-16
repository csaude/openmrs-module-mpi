package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.debezium.DatabaseOperation.UPDATE;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_AUTHENTICATION_TYPE;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_MPI_APP_CONTENT_TYPE;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_MPI_SYSTEM;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_SANTE_CLIENT_ID;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_SANTE_CLIENT_SECRET;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_SANTE_MESSAGE_HEADER_EVENT_URI;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_UUID_SYSTEM;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.OPENMRS_UUID;
import static org.openmrs.module.fgh.mpi.processor.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.processor.MpiIntegrationProcessor.PATIENT_QUERY;
import static org.openmrs.module.fgh.mpi.processor.MpiIntegrationProcessor.PERSON_QUERY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.fgh.mpi.entity.AuthenticationType;
import org.openmrs.module.fgh.mpi.entity.MpiSystemType;
import org.openmrs.module.fgh.mpi.handler.PatientAndPersonEventHandler;
import org.openmrs.module.fgh.mpi.integ.MpiContext;
import org.openmrs.module.fgh.mpi.integ.MpiHttpClient;
import org.openmrs.module.fgh.mpi.processor.BaseEventProcessor;
import org.openmrs.module.fgh.mpi.processor.SnapshotEventProcessor;
import org.openmrs.module.fgh.mpi.utils.FhirUtils;
import org.openmrs.module.fgh.mpi.utils.MpiUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, FhirUtils.class, MpiContext.class, BaseEventProcessor.class,
        KeyManagerFactory.class })
public class SnapshotEventProcessorTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private AdministrationService adminService;
	
	private static final String UUID_SYSTEM = "http://test.openmrs.id/uuid";
	
	private static final String MESSAGE_HEADER_REFERENCE = "metadata.epts.e-saude.net/bundle";
	
	private static final String MESSAGE_HEADER_EVENT_URI = "urn:ihe:iti:pmir:2019:patient-feed";
	
	private static final AuthenticationType AUTHENTICATION_TYPE = AuthenticationType.OAUTH;
	
	private static final String MPI_BASE_URL = "http://sante.org.mz";
	
	private static final String MPI_APP_CONTENT_TYPE = "application/fhir+json";
	
	private static final MpiSystemType MPI_SYSTEM = MpiSystemType.SANTEMPI;
	
	private static final String SANTE_CLIENT_ID = "client_credentials";
	
	private static final String SANTE_CLIENT_SECRET = "bG6TuS3X-H1MsT4ctW!CxXjK9J4l1QpK8B0Q";
	
	@Mock
	private MpiContext mpiContext;
	
	@Mock
	private Context context;
	
	@Captor
	private ArgumentCaptor<DatabaseEvent> DatabaseEventCaptor;
	
	@Before
	public void setup() throws Exception {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(FhirUtils.class);
		PowerMockito.mockStatic(MpiContext.class);
		PowerMockito.mockStatic(KeyStore.class);
		PowerMockito.mockStatic(KeyManagerFactory.class);
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
	public void process_shouldIntegrateWithOpenCr() throws Exception {
		PatientAndPersonEventHandler mockHandler = mock(PatientAndPersonEventHandler.class);
		MpiHttpClient mockMpiHttpClient = mock(MpiHttpClient.class);
		when(Context.getRegisteredComponents(PatientAndPersonEventHandler.class))
		        .thenReturn(Collections.singletonList(mockHandler));
		when(Context.getRegisteredComponents(MpiHttpClient.class)).thenReturn(Collections.singletonList(mockMpiHttpClient));
		
		AuthenticationType CERTIFICATE = AuthenticationType.CERTIFICATE;
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(CERTIFICATE.toString());
		SnapshotEventProcessor snapshotEventProcessor = new SnapshotEventProcessor(2);
		final String patientUuid = "patient-uuid-for-openCR";
		Map res = new HashMap();
		res.put("active", true);
		res.put(OPENMRS_UUID, patientUuid);
		res.put("id", 1);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(res);
		Map prevState = singletonMap("uuid", patientUuid);
		final Integer patientId = 1;
		
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		
		List<List<Object>> expectedPatient = new ArrayList<>();
		expectedPatient.add(asList(patientUuid));
		when(MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString()))).thenReturn(expectedPatient);
		Map<String, Object> patinetGeneratedPayload = new HashMap<>();
		patinetGeneratedPayload.put("name", "mpi");
		when(FhirUtils.buildPatient(patientId.toString(), false, expectedPatient.get(0), patinetGeneratedPayload))
		        .thenReturn(patinetGeneratedPayload);
		snapshotEventProcessor = Mockito.spy(snapshotEventProcessor);
		snapshotEventProcessor.process(new DatabaseEvent(patientId, "person", UPDATE, null, prevState, null));
		
		verify(snapshotEventProcessor).process(DatabaseEventCaptor.capture());
		DatabaseEvent submitedDataBaseEvent = DatabaseEventCaptor.getValue();
		assertEquals(submitedDataBaseEvent.getPrimaryKeyId(), submitedDataBaseEvent.getPrimaryKeyId());
		assertEquals(submitedDataBaseEvent.getTableName(), submitedDataBaseEvent.getTableName());
		assertEquals(submitedDataBaseEvent.getOperation(), submitedDataBaseEvent.getOperation());
	}
	
	@Test
	public void process_shouldIntegrateWithSanteMpi() throws Exception {
		PatientAndPersonEventHandler mockHandler = mock(PatientAndPersonEventHandler.class);
		MpiHttpClient mockMpiHttpClient = mock(MpiHttpClient.class);
		when(Context.getRegisteredComponents(PatientAndPersonEventHandler.class))
		        .thenReturn(Collections.singletonList(mockHandler));
		when(Context.getRegisteredComponents(MpiHttpClient.class)).thenReturn(Collections.singletonList(mockMpiHttpClient));
		
		SnapshotEventProcessor snapshotEventProcessor = new SnapshotEventProcessor(2);
		AuthenticationType OUAUTH = AuthenticationType.OAUTH;
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(OUAUTH.toString());
		when(context.getRegisteredComponents(PatientAndPersonEventHandler.class)).thenReturn(null);
		when(context.getRegisteredComponents(MpiHttpClient.class)).thenReturn(null);
		final String patientUuid = "patient-uuid-for-openCR";
		Map res = new HashMap();
		res.put("active", true);
		res.put(OPENMRS_UUID, patientUuid);
		res.put("id", 1);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(res);
		Map prevState = singletonMap("uuid", patientUuid);
		final Integer patientId = 1;
		
		when(MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, patientId.toString())))
		        .thenReturn(asList(asList(null, null, null, null, patientUuid, null)));
		
		List<List<Object>> expectedPatient = new ArrayList<>();
		expectedPatient.add(asList(patientUuid));
		when(MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, patientId.toString()))).thenReturn(expectedPatient);
		Map<String, Object> patinetGeneratedPayload = new HashMap<>();
		patinetGeneratedPayload.put("name", "mpi");
		when(FhirUtils.buildPatient(patientId.toString(), false, expectedPatient.get(0), patinetGeneratedPayload))
		        .thenReturn(patinetGeneratedPayload);
		snapshotEventProcessor = Mockito.spy(snapshotEventProcessor);
		snapshotEventProcessor.process(new DatabaseEvent(patientId, "person", UPDATE, null, prevState, null));
		
		verify(snapshotEventProcessor).process(DatabaseEventCaptor.capture());
		DatabaseEvent submitedDataBaseEvent = DatabaseEventCaptor.getValue();
		assertEquals(submitedDataBaseEvent.getPrimaryKeyId(), submitedDataBaseEvent.getPrimaryKeyId());
		assertEquals(submitedDataBaseEvent.getTableName(), submitedDataBaseEvent.getTableName());
		assertEquals(submitedDataBaseEvent.getOperation(), submitedDataBaseEvent.getOperation());
	}
}
