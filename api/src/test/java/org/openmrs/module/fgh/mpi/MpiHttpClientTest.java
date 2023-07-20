package org.openmrs.module.fgh.mpi;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.openmrs.module.fgh.mpi.MpiConstants.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, MpiContext.class, URL.class })
public class MpiHttpClientTest {
	
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
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Mock
	private MpiContext mpiContext;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(MpiContext.class);
		PowerMockito.mockStatic(URL.class);
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
		mpiContext.setAuthenticationType(AUTHENTICATION_TYPE.CERTIFICATE);
		
	}
	
	@Test
	public void getPatient_shouldRetrievePatientInOpenCR() throws Exception {
		
		final String patientUuid = "patient-uuid-open-cr";
		String baseUrl = "https://localhost:8080";
		Map<String, Object> patientResponse = new HashMap<>();
		patientResponse.put(FIELD_ACTIVE, true);
		patientResponse.put(OPENMRS_UUID, patientUuid);
		
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.CERTIFICATE);
		when(mpiContext.getServerBaseUrl()).thenReturn(baseUrl);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(patientResponse);
		Map<String, Object> openCRResponse = mockMpiHttpClient.getPatient(patientUuid);
		
		assertEquals(openCRResponse.get(FIELD_ACTIVE), patientResponse.get(FIELD_ACTIVE));
		assertEquals(openCRResponse.get(OPENMRS_UUID), patientUuid);
	}
	
	@Test
	public void getPatient_shouldRetrievePatientInSanteMpi() throws Exception {
		final String patientUuid = "patient-uuid-sante-mpi";
		String baseUrl = "https://localhost:8080";
		Map<String, Object> patientResponse = new HashMap<>();
		patientResponse.put(FIELD_ACTIVE, true);
		patientResponse.put(OPENMRS_UUID, patientUuid);
		
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.OAUTH);
		when(mockMpiHttpClient.getPatient(patientUuid)).thenReturn(patientResponse);
		doNothing().when(mockMpiHttpClient).doAuthentication(any(String.class));
		Map<String, Object> santeMPIResponse = mockMpiHttpClient.getPatient(patientUuid);
		
		assertEquals(santeMPIResponse.get(FIELD_ACTIVE), patientResponse.get(FIELD_ACTIVE));
		assertEquals(santeMPIResponse.get(OPENMRS_UUID), patientUuid);
		
	}
	
	@Test
	public void submitBundle_shouldRetrieveAccessTokenAndSubmitBundle() throws Exception {
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
		List<Object> submutedData = mockMpiHttpClient.submitBundle("/fhiir-url/test", bundleData, List.class);
		assertNotNull(submutedData);
	}
	
	@Test
	public void submitPatient_shouldSubmitPatientAsOpenCr() throws Exception {
		
		final String patientUuid = "patient-uuid-opencr";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(OPENMRS_UUID, patientUuid);
		List<Map<String, Object>> mpiIdsResp = new ArrayList<>();
		when(mockMpiHttpClient.submitRequest("/fhiir-url/test", patientData.toString(), List.class)).thenReturn(mpiIdsResp);
		this.mockMpiHttpClient.submitPatient(patientData.toString());
		when(mpiContext.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.CERTIFICATE);
		
		doAnswer(invocation -> {
			Map<String, Object> capturedPatientData = (Map<String, Object>) invocation.getArguments()[0];
			
			assertEquals(capturedPatientData.get(FIELD_ACTIVE), patientData.get(FIELD_ACTIVE));
			assertEquals(capturedPatientData.get(OPENMRS_UUID), patientData.get(OPENMRS_UUID));
			return capturedPatientData;
		}).when(mockMpiHttpClient).submitPatient(patientData.toString());
		
	}
	
	@Test
	public void submitPatient_shouldSubmitPatientAsSanteMpi() throws Exception {
		
		final String patientUuid = "patient-uuid-santempi";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(OPENMRS_UUID, patientUuid);
		List<Map<String, Object>> mpiIdsResp = new ArrayList<>();
		when(mockMpiHttpClient.submitRequest("/fhiir-url/test", patientData.toString(), List.class)).thenReturn(mpiIdsResp);
		this.mockMpiHttpClient.submitPatient(patientData.toString());
		when(mpiContext.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.OAUTH);
		
		doAnswer(invocation -> {
			Map<String, Object> capturedPatientData = (Map<String, Object>) invocation.getArguments()[0];
			
			assertEquals(capturedPatientData.get(FIELD_ACTIVE), patientData.get(FIELD_ACTIVE));
			assertEquals(capturedPatientData.get(OPENMRS_UUID), patientData.get(OPENMRS_UUID));
			return capturedPatientData;
		}).when(mockMpiHttpClient).submitPatient(patientData.toString());
	}
}
