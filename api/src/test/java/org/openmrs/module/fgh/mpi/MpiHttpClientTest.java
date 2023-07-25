package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ACTIVE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_AUTHENTICATION_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_APP_CONTENT_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_SECRET;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_EVENT_URI;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.OPENMRS_UUID;
import static java.util.Collections.singletonMap;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, MpiContext.class, URL.class })
public class MpiHttpClientTest {
	
	private MpiHttpClient mockMpiHttpClient = new MpiHttpClient();

	@Mock
	private HttpURLConnection httpURLConnection;
	
	@Mock
	private AdministrationService adminService;

	private static final String TOKEN_ID = "TOKEN-ID";

	private static final String ACCESS_TOKEN = "ACCESS-TOKEN";

	private static final String REFRESH_TOKEN = "REFRESH_TOKEN";
	
	private static final String UUID_SYSTEM = "http://test.openmrs.id/uuid";
	
	private static final String MESSAGE_HEADER_REFERENCE = "metadata.epts.e-saude.net/bundle";
	
	private static final String MESSAGE_HEADER_EVENT_URI = "urn:ihe:iti:pmir:2019:patient-feed";
	
	private static final AuthenticationType AUTHENTICATION_TYPE = AuthenticationType.OAUTH;
	
	private static final String MPI_BASE_URL = "https://demompi.santesuite.net";
	
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
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
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
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(mpiContext.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.CERTIFICATE);
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
	public void submitPatient_shouldSubmitPatientAsSanteMpi() throws Exception {

		MpiHttpClient mockClient = PowerMockito.mock(MpiHttpClient.class);
		// Create an TokenInfo class to skip do authentication for santeMPI
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500,  ChronoUnit.MILLIS));
		tokenInfo.setExpiresIn(140l);

		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getTokenInfo()).thenReturn(tokenInfo);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AuthenticationType.OAUTH);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
		doNothing().when(mockClient).handleUnexpectedResponse(anyInt(), anyString());
		when(httpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

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
	public void submitPatient_shouldSubmitPatientAsOPenCr() throws Exception {

		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AuthenticationType.CERTIFICATE);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);

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
