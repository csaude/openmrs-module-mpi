package org.openmrs.module.fgh.mpi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;
import static org.openmrs.module.debezium.DatabaseOperation.UPDATE;
import static org.openmrs.module.fgh.mpi.MpiConstants.*;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, FhirUtils.class, MpiContext.class, BaseEventProcessor.class, KeyManagerFactory.class })
public class SnapshotEventProcessorTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private SnapshotEventProcessor snapshotEventProcessor;
	
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
	private Context context = new Context();
	
	@Before
	public void setup() throws Exception {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(FhirUtils.class);
		PowerMockito.mockStatic(MpiContext.class);
		PowerMockito.mockStatic(KeyStore.class);
		PowerMockito.mockStatic(KeyManagerFactory.class);
		Whitebox.setInternalState(snapshotEventProcessor, MpiHttpClient.class, mockMpiHttpClient);
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
		AuthenticationType CERTIFICATE = AuthenticationType.CERTIFICATE;
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(CERTIFICATE.toString());
		final String patientUuid = "patient-uuid-for-openCR";
		Map res = new HashMap();
		res.put("active", true);
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
		snapshotEventProcessor.process(new DatabaseEvent(null, "person", UPDATE, null, prevState, null));
		Mockito.verify(mpiContext, Mockito.never()).initOauth();
		
	}
	
	@Test
	public void process_shouldIntegrateWithSanteMpi() throws Exception {
		AuthenticationType OUAUTH = AuthenticationType.OAUTH;
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(OUAUTH.toString());
		when(context.getRegisteredComponents(PatientAndPersonEventHandler.class)).thenReturn(null);
		when(context.getRegisteredComponents(MpiHttpClient.class)).thenReturn(null);
		final String patientUuid = "patient-uuid-for-openCR";
		Map res = new HashMap();
		res.put("active", true);
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
		snapshotEventProcessor.process(new DatabaseEvent(null, "person", UPDATE, null, prevState, null));
		Mockito.verify(mpiContext, Mockito.never()).initSSL();
	}
}