package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
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
import static org.openmrs.module.fgh.mpi.MpiConstants.RESPONSE_FIELD_PARAM;
import static org.openmrs.module.fgh.mpi.MpiConstants.RESPONSE_FIELD_VALUE_REF;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.codehaus.jackson.map.ObjectMapper;
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
	
	private MpiHttpClient mpiHttpClient = new MpiHttpClient();

	@Mock
	private HttpURLConnection httpURLConnectionMock;

	@Mock
	private HttpsURLConnection httpsURLConnectionMock;
	
	@Mock
	private AdministrationService adminService;

	private static final String TOKEN_ID = "TOKEN_ID";

	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";

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
		// Mock for HttpURLConnection
		String baseUrl = "https://localhost:8080";
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.CERTIFICATE);
		when(mpiContext.getServerBaseUrl()).thenReturn(baseUrl);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);
		when(MpiUtils.openConnectionForSSL("https://demompi.santesuite.net//fhiir-url/test", mpiContext)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("https://demompi.santesuite.net/auth/oauth2_token", mpiContext)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("https://demompi.santesuite.net/fhir/Patient", mpiContext)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("https://localhost:8080/fhir/Patient/$ihe-pix?sourceIdentifier=null|patient-uuid-open-cr", mpiContext)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("https://localhost:8080/fhir/Patient/[]", mpiContext)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("https://localhost:8080/fhir/Patient/opencr", mpiContext)).thenReturn(httpsURLConnectionMock);

		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpsURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpsURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);

		Map<String, Object> pixResponse = new HashMap<>();

		List<Map<String, Object>> patienIds = new ArrayList<>();
		Map<String, Object> patientId = new HashMap<>();
		patientId.put("id", 1);
		patientId.put(RESPONSE_FIELD_VALUE_REF, "opencr");
		patienIds.add(patientId);

		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);

		ObjectMapper objectMapper = new ObjectMapper();

		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpsURLConnectionMock.getInputStream()).thenReturn(inputStream);

		final String patientUuid = "patient-uuid-open-cr";
		Map<String, Object> patientResponse = new HashMap<>();
		patientResponse.put(FIELD_ACTIVE, true);
		patientResponse.put(OPENMRS_UUID, patientUuid);
		Map<String, Object> openCRResponse = mpiHttpClient.getPatient(patientUuid);
		
		assertEquals(openCRResponse.get(FIELD_ACTIVE), patientResponse.get(FIELD_ACTIVE));
		assertEquals(openCRResponse.get(OPENMRS_UUID), patientUuid);
	}
	
	@Test
	public void getPatient_shouldRetrievePatientInSanteMpi() throws Exception {
		// Mock for HttpURLConnection
		when(MpiUtils.openConnection("https://demompi.santesuite.net//fhiir-url/test")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("https://demompi.santesuite.net/auth/oauth2_token")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("https://demompi.santesuite.net/fhir/Patient")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("https://demompi.santesuite.net/fhir/Patient/$ihe-pix?sourceIdentifier=null|patient-uuid-santempi")).thenReturn(httpURLConnectionMock);
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

		// Mock inputSTream for mock connection
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500,  ChronoUnit.MILLIS));
		tokenInfo.setExpiresIn(140l);
		tokenInfo.setTokenType("bearer");

		Map<String, Object> pixResponse = new HashMap<>();
		List<Map<String, Object>> patienIds = new ArrayList<>();
		Map<String, Object> patientId = new HashMap<>();
		patientId.put("id", 1);
		patientId.put(RESPONSE_FIELD_VALUE_REF, "sante");
		patienIds.add(patientId);

		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);

		ObjectMapper objectMapper = new ObjectMapper();

		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpsURLConnectionMock.getInputStream()).thenReturn(inputStream);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getTokenInfo()).thenReturn(tokenInfo);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AuthenticationType.OAUTH);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);

		final String patientUuid = "patient-uuid-santempi";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(OPENMRS_UUID, patientUuid);

		Map<String, Object> santeMPIResponse =  mpiHttpClient.getPatient(patientUuid);

		assertEquals(santeMPIResponse.get(FIELD_ACTIVE), patientData.get(FIELD_ACTIVE));
		assertEquals(santeMPIResponse.get(OPENMRS_UUID), patientUuid);
		
	}
	
	@Test
	public void submitBundle_shouldRetrieveAccessTokenAndSubmitBundle() throws Exception {
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(mpiContext.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.OAUTH);
		final String patientUuid = "patient-uuid";
		when(MpiUtils.openConnection("https://demompi.santesuite.net/fhir/Patient/$ihe-pix?sourceIdentifier=null|patient-uuid")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("https://demompi.santesuite.net//fhiir-url/test")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("https://demompi.santesuite.net/auth/oauth2_token")).thenReturn(httpURLConnectionMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);


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
		when(mpiHttpClient.submitBundle("/fhiir-url/test", bundleData, List.class)).thenReturn(response);
		List<Object> submutedData = mpiHttpClient.submitBundle("/fhiir-url/test", bundleData, List.class);
		assertNotNull(submutedData);
	}
	
	@Test
	public void submitPatient_shouldSubmitPatientAsSanteMpi() throws Exception {
		// Mock for HttpURLConnection
		when(MpiUtils.openConnection("https://demompi.santesuite.net//fhiir-url/test")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("https://demompi.santesuite.net/auth/oauth2_token")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("https://demompi.santesuite.net/fhir/Patient")).thenReturn(httpURLConnectionMock);
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

		// Mock inputSTream for mock connection
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500,  ChronoUnit.MILLIS));
		tokenInfo.setExpiresIn(140l);
		tokenInfo.setTokenType("bearer");

		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> tokenInfoAsJson = new HashMap<>();
		tokenInfoAsJson.put("access_token", ACCESS_TOKEN);
		tokenInfoAsJson.put("id_token", TOKEN_ID);
		tokenInfoAsJson.put("expires_in", "140");
		tokenInfoAsJson.put("token_type", "bearer");
		tokenInfoAsJson.put("refresh_token", REFRESH_TOKEN);

		String jsonString = objectMapper.writeValueAsString(tokenInfoAsJson);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());

		when(httpURLConnectionMock.getInputStream()).thenReturn(inputStream);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getTokenInfo()).thenReturn(tokenInfo);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AuthenticationType.OAUTH);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);

		final String patientUuid = "patient-uuid-santempi";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(OPENMRS_UUID, patientUuid);

		this.mpiHttpClient.submitPatient(patientData.toString());

		MpiHttpClient httpClientMock = PowerMockito.mock(MpiHttpClient.class);
		doAnswer(invocation -> {
			Map<String, Object> capturedPatientData = (Map<String, Object>) invocation.getArguments()[0];
			
			assertEquals(capturedPatientData.get(FIELD_ACTIVE), patientData.get(FIELD_ACTIVE));
			assertEquals(capturedPatientData.get(OPENMRS_UUID), patientData.get(OPENMRS_UUID));
			return capturedPatientData;
		}).when(httpClientMock).submitPatient(patientData.toString());
		
	}
	
	@Test
	public void submitPatient_shouldSubmitPatientAsOPenCr() throws Exception {
		// Mock for HttpURLConnection
		when(MpiUtils.openConnectionForSSL("https://demompi.santesuite.net//fhiir-url/test", mpiContext)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("https://demompi.santesuite.net/auth/oauth2_token", mpiContext)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("https://demompi.santesuite.net/fhir/Patient", mpiContext)).thenReturn(httpsURLConnectionMock);
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpsURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpsURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(mpiContext.getAuthenticationType()).thenReturn(AuthenticationType.CERTIFICATE);
		when(mpiContext.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);

		SSLContext sslContextMock = PowerMockito.mock(SSLContext.class);
		mpiContext.setSslContext(sslContextMock);
		when(mpiContext.getSslContext()).thenReturn(sslContextMock);

		ObjectMapper objectMapper = new ObjectMapper();

		List<Map<String, Object>> patienIds = new ArrayList<>();
		Map<String, Object> patientId = new HashMap<>();
		patientId.put("id", 1);
		patienIds.add(patientId);

		String jsonString = objectMapper.writeValueAsString(patienIds);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpsURLConnectionMock.getInputStream()).thenReturn(inputStream);

		final String patientUuid = "patient-uuid-opencr";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(OPENMRS_UUID, patientUuid);
		MpiHttpClient httpClientMock = PowerMockito.mock(MpiHttpClient.class);
		this.mpiHttpClient.submitPatient(patientData.toString());

		doAnswer(invocation -> {
			Map<String, Object> capturedPatientData = (Map<String, Object>) invocation.getArguments()[0];
			
			assertEquals(capturedPatientData.get(FIELD_ACTIVE), patientData.get(FIELD_ACTIVE));
			assertEquals(capturedPatientData.get(OPENMRS_UUID), patientData.get(OPENMRS_UUID));
			return capturedPatientData;
		}).when(httpClientMock).submitPatient(patientData.toString());
	}
}
