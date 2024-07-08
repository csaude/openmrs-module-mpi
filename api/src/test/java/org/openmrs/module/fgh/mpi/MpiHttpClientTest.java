package org.openmrs.module.fgh.mpi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ACTIVE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CONTACT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_RELATIONSHIP;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_VALUE_UUID;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, MpiContext.class, URL.class, LoggerFactory.class })
@SuppressStaticInitializationFor({"org.openmrs.module.fgh.mpi.MpiContext", ""})
@PowerMockIgnore("javax.management.*")
public class MpiHttpClientTest {
	
	private MpiHttpClient mpiHttpClient;
	
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
	
	private static final String MPI_BASE_URL = "sante.fake";
	
	private static final String MPI_APP_CONTENT_TYPE = "application/fhir+json";
	
	private static final MpiSystemType MPI_SYSTEM = MpiSystemType.SANTEMPI;
	
	private static final String SANTE_CLIENT_ID = "client_credentials";
	
	private static final String SANTE_CLIENT_SECRET = "bG6TuS3X-H1MsT4ctW!CxXjK9J4l1QpK8B0Q";
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Mock
	private MpiContext mpiContextMock;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(LoggerFactory.class);
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(MpiContext.class);
		PowerMockito.mockStatic(URL.class);
		when(LoggerFactory.getLogger(any(Class.class))).thenReturn(Mockito.mock(Logger.class));
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
		mpiHttpClient = new MpiHttpClient();
		
	}
	
	@Test
	public void doAuthentication_shouldDoAuthenticationForNonLoggedUser() throws Exception {
		// Mock for HttpURLConnection
		MpiContext mpiContext = new MpiContext();
		mpiContext.setServerBaseUrl(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContext);
		when(MpiUtils.openConnection("sante.fake/auth/oauth2_token")).thenReturn(httpURLConnectionMock);
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		
		// Mock inputSTream for mock connection
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500, ChronoUnit.MILLIS));
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
		
		Map<String, Object> authenticationData = new HashMap<>();
		authenticationData.put("grant_type", "client_credential");
		authenticationData.put("scope", "*");
		authenticationData.put("client_secret", GP_SANTE_CLIENT_SECRET);
		authenticationData.put("client_id", GP_SANTE_CLIENT_ID);
		String authenticationDataAsString = objectMapper.writeValueAsString(authenticationData);
		
		this.mpiHttpClient.doAuthentication(authenticationDataAsString);
		assertEquals(mpiContext.getTokenInfo().getAccessToken(), tokenInfoAsJson.get("access_token"));
		assertEquals(mpiContext.getTokenInfo().getTokenId(), tokenInfoAsJson.get("id_token"));
		assertEquals(mpiContext.getTokenInfo().getTokenType(), tokenInfoAsJson.get("token_type"));
		assertEquals(mpiContext.getTokenInfo().getExpiresIn(), Long.parseLong(tokenInfoAsJson.get("expires_in")));
		assertEquals(mpiContext.getTokenInfo().getRefreshToken(), tokenInfoAsJson.get("refresh_token"));
	}
	
	@Test
	public void submitRequest_shouldDoSubmitRequestForSantMpi() throws Exception {
		// Mock for HttpURLConnection
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.OAUTH);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
		when(mpiContextMock.getClientId()).thenReturn(SANTE_CLIENT_ID);
		when(mpiContextMock.getClientSecret()).thenReturn(SANTE_CLIENT_SECRET);
		when(MpiUtils.openConnection("sante.fake/fhir/Patient")).thenReturn(httpURLConnectionMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500, ChronoUnit.MILLIS));
		tokenInfo.setExpiresIn(140l);
		tokenInfo.setTokenType("bearer");
		when(mpiContextMock.getTokenInfo()).thenReturn(tokenInfo);
		
		Map<String, Object> pixResponse = new HashMap<>();
		List<Map<String, Object>> patienIds = new ArrayList<>();
		String patientUuid = "d454b7b4-2c85-11ee-fake-0242ac120002";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put("id", 1);
		patientData.put(RESPONSE_FIELD_VALUE_REF, patientUuid);
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(FIELD_VALUE_UUID, patientUuid);
		patienIds.add(patientData);
		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpURLConnectionMock.getInputStream()).thenReturn(inputStream);
		
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		doNothing().when(mpiHttpClient).doAuthentication(
		    "grant_type=client_credentials&scope=*&client_secret=bG6TuS3X-H1MsT4ctW!CxXjK9J4l1QpK8B0Q&client_id=client_credentials");
		
		Map<String, Object> submitedData = this.mpiHttpClient.submitRequest("fhir/Patient", null, Map.class);
		Map<String, Object> response = (Map<String, Object>) ((List) submitedData.get(RESPONSE_FIELD_PARAM)).get(0);
		
		assertEquals(pixResponse.size(), submitedData.size());
		assertEquals(((List) submitedData.get(RESPONSE_FIELD_PARAM)).get(0),
		    ((List) pixResponse.get(RESPONSE_FIELD_PARAM)).get(0));
		assertEquals(response.get(RESPONSE_FIELD_VALUE_REF), patientUuid);
		assertEquals(response.get(FIELD_ACTIVE), true);
	}
	
	@Test
	public void submitRequest_shouldDoSubmitRequestForOpenCr() throws Exception {
		// Mock for HttpURLConnection
		String baseUrl = "opencr.fake";
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.CERTIFICATE);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(baseUrl);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);
		when(MpiUtils.openConnectionForSSL("opencr.fake/fhir/Patient", mpiContextMock)).thenReturn(httpsURLConnectionMock);
		when(httpsURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		
		Map<String, Object> pixResponse = new HashMap<>();
		List<Map<String, Object>> patienIds = new ArrayList<>();
		String patientUuid = "d454b7b4-2c85-11ee-fake-0242ac120002";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put("id", 1);
		patientData.put(RESPONSE_FIELD_VALUE_REF, patientUuid);
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(FIELD_VALUE_UUID, patientUuid);
		patienIds.add(patientData);
		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpsURLConnectionMock.getInputStream()).thenReturn(inputStream);
		
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		
		Map<String, Object> submitedData = this.mpiHttpClient.submitRequest("fhir/Patient", null, Map.class);
		Map<String, Object> response = (Map<String, Object>) ((List) submitedData.get(RESPONSE_FIELD_PARAM)).get(0);
		
		assertEquals(pixResponse.size(), submitedData.size());
		assertEquals(((List) submitedData.get(RESPONSE_FIELD_PARAM)).get(0),
		    ((List) pixResponse.get(RESPONSE_FIELD_PARAM)).get(0));
		assertEquals(response.get(RESPONSE_FIELD_VALUE_REF), patientUuid);
		assertEquals(response.get(FIELD_ACTIVE), true);
	}
	
	@Test
	public void submitRequest_shouldHandleUnexpectedResponseForSante() throws Exception {
		// Mock for HttpURLConnection
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.OAUTH);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
		when(mpiContextMock.getClientId()).thenReturn(SANTE_CLIENT_ID);
		when(mpiContextMock.getClientSecret()).thenReturn(SANTE_CLIENT_SECRET);
		when(MpiUtils.openConnection("sante.fake/fhir/Patient")).thenReturn(httpURLConnectionMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
		
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500, ChronoUnit.MILLIS));
		tokenInfo.setExpiresIn(140l);
		tokenInfo.setTokenType("bearer");
		when(mpiContextMock.getTokenInfo()).thenReturn(tokenInfo);
		
		Map<String, Object> pixResponse = new HashMap<>();
		List<Map<String, Object>> patienIds = new ArrayList<>();
		String patientUuid = "d454b7b4-2c85-11ee-fake-0242ac120002";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put("id", 1);
		patientData.put(RESPONSE_FIELD_VALUE_REF, patientUuid);
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(FIELD_VALUE_UUID, patientUuid);
		patienIds.add(patientData);
		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpURLConnectionMock.getInputStream()).thenReturn(inputStream);
		
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		doNothing().when(mpiHttpClient).doAuthentication(
		    "grant_type=client_credentials&scope=*&client_secret=bG6TuS3X-H1MsT4ctW!CxXjK9J4l1QpK8B0Q&client_id=client_credentials");
		
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("Unexpected response 406 null from MPI"));
		this.mpiHttpClient.submitRequest("fhir/Patient", null, Map.class);
	}
	
	@Test
	public void submitRequest_shouldHandleUnexpectedResponseForOpenCr() throws Exception {
		// Mock for HttpURLConnection
		String baseUrl = "opencr.fake";
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.CERTIFICATE);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(baseUrl);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);
		when(MpiUtils.openConnectionForSSL("opencr.fake/fhir/Patient", mpiContextMock)).thenReturn(httpsURLConnectionMock);
		when(httpsURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
		
		Map<String, Object> pixResponse = new HashMap<>();
		List<Map<String, Object>> patienIds = new ArrayList<>();
		String patientUuid = "d454b7b4-2c85-11ee-fake-0242ac120002";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put("id", 1);
		patientData.put(RESPONSE_FIELD_VALUE_REF, patientUuid);
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(FIELD_VALUE_UUID, patientUuid);
		patienIds.add(patientData);
		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpsURLConnectionMock.getInputStream()).thenReturn(inputStream);
		
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("Unexpected response 404 null from MPI"));
		this.mpiHttpClient.submitRequest("fhir/Patient", null, Map.class);
	}
	
	@Test
	public void getPatient_shouldRetrievePatientInOpenCR() throws Exception {
		// Mock for HttpURLConnection
		String baseUrl = "opencr.fake";
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.CERTIFICATE);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(baseUrl);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);
		
		Map<String, Object> pixResponse = new HashMap<>();
		List<Map<String, Object>> patienIds = new ArrayList<>();
		String patientUuid = "d454b7b4-2c85-11ee-fake-0242ac120002";
		Map<String, Object> patientData = new HashMap<>();
		patientData.put("id", 1);
		patientData.put(RESPONSE_FIELD_VALUE_REF, patientUuid);
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(FIELD_VALUE_UUID, patientUuid);
		patienIds.add(patientData);
		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpsURLConnectionMock.getInputStream()).thenReturn(inputStream);
		
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		
		doAnswer(invocation -> {
			Map updatedContact1 = new HashMap();
			updatedContact1.put(FIELD_RELATIONSHIP, null);
			Map updatedContact2 = new HashMap();
			updatedContact2.put(FIELD_RELATIONSHIP, null);
			
			Map expectedMpiPatient = new HashMap();
			expectedMpiPatient.put(FIELD_ACTIVE, true);
			expectedMpiPatient.put(FIELD_VALUE_UUID, "d454b7b4-2c85-11ee-fake-0242ac120002");
			expectedMpiPatient.put(FIELD_CONTACT,
			    asList(updatedContact1, singletonMap(FIELD_RELATIONSHIP, emptyMap()), updatedContact2));
			return expectedMpiPatient;
		}).when(mpiHttpClient).submitRequest("fhir/Patient/d454b7b4-2c85-11ee-fake-0242ac120002", null, Map.class);
		doAnswer(invocation -> {
			return pixResponse;
		}).when(mpiHttpClient).submitRequest(
		    "fhir/Patient/$ihe-pix?sourceIdentifier=null|d454b7b4-2c85-11ee-fake-0242ac120002", null, Map.class);
		
		Map<String, Object> openCRResponse = mpiHttpClient.getPatient(patientUuid);
		
		assertEquals(openCRResponse.get(FIELD_ACTIVE), patientData.get(FIELD_ACTIVE));
		assertEquals(openCRResponse.get(FIELD_VALUE_UUID), patientUuid);
	}
	
	@Test
	public void getPatient_shouldRetrievePatientInSanteMpi() throws Exception {
		
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.OAUTH);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
		
		// Mock inputSTream for mock connection
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500, ChronoUnit.MILLIS));
		tokenInfo.setExpiresIn(140l);
		tokenInfo.setTokenType("bearer");
		
		final String patientUuid = "d454b7b4-2c85-11ee-fake-0242ac120002";
		
		Map<String, Object> pixResponse = new HashMap<>();
		List<Map<String, Object>> patienIds = new ArrayList<>();
		Map<String, Object> patientId = new HashMap<>();
		patientId.put("id", 1);
		patientId.put(RESPONSE_FIELD_VALUE_REF, "/" + patientUuid);
		patienIds.add(patientId);
		
		pixResponse.put(RESPONSE_FIELD_PARAM, patienIds);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(pixResponse);
		InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());
		when(httpsURLConnectionMock.getInputStream()).thenReturn(inputStream);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getTokenInfo()).thenReturn(tokenInfo);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AuthenticationType.OAUTH);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
		
		Map<String, Object> patientData = new HashMap<>();
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(FIELD_VALUE_UUID, patientUuid);
		
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		doNothing().when(mpiHttpClient)
		        .doAuthentication("grant_type=refresh_token&refresh_token=REFRESH_TOKEN&client_secret=null&client_id=null");
		doAnswer(invocation -> {
			Map updatedContact1 = new HashMap();
			updatedContact1.put(FIELD_RELATIONSHIP, null);
			Map updatedContact2 = new HashMap();
			updatedContact2.put(FIELD_RELATIONSHIP, null);
			
			Map expectedMpiPatient = new HashMap();
			expectedMpiPatient.put(FIELD_ACTIVE, true);
			expectedMpiPatient.put(FIELD_VALUE_UUID, "d454b7b4-2c85-11ee-fake-0242ac120002");
			expectedMpiPatient.put(FIELD_CONTACT,
			    asList(updatedContact1, singletonMap(FIELD_RELATIONSHIP, emptyMap()), updatedContact2));
			return expectedMpiPatient;
			
		}).when(mpiHttpClient).submitRequest("fhir/Patient/d454b7b4-2c85-11ee-fake-0242ac120002", null, Map.class);
		doAnswer(invocation -> {
			return pixResponse;
		}).when(mpiHttpClient).submitRequest(
		    "fhir/Patient/$ihe-pix?sourceIdentifier=null|d454b7b4-2c85-11ee-fake-0242ac120002", null, Map.class);
		
		when(mpiHttpClient.submitRequest("fhir/Patient/$ihe-pix?sourceIdentifier=null|d454b7b4-2c85-11ee-fake-0242ac120002",
		    null, Map.class)).thenReturn(pixResponse);
		Map<String, Object> santeMPIResponse = mpiHttpClient.getPatient(patientUuid);
		
		assertEquals(santeMPIResponse.get(FIELD_ACTIVE), patientData.get(FIELD_ACTIVE));
		assertEquals(santeMPIResponse.get(FIELD_VALUE_UUID), patientUuid);
	}
	
	@Test
	public void submitBundle_shouldRetrieveAccessTokenAndSubmitBundle() throws Exception {
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AUTHENTICATION_TYPE.OAUTH);
		final String patientAUuid = "d454b7b4-2c85-11ee-pata-0242ac120002";
		final String patientBUuid = "d454b7b4-2c85-11ee-patb-0242ac120002";
		
		when(MpiUtils.openConnection("sante.fake//fhiir-url/test")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("sante.fake/auth/oauth2_token")).thenReturn(httpURLConnectionMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
		
		Map<String, Object> messageHeader = FhirUtils.generateMessageHeader();
		List<Map<String, Object>> entryList = new ArrayList<>();
		
		Map<String, Object> patientA = new HashMap<>();
		patientA.put(FIELD_ACTIVE, true);
		patientA.put(FIELD_VALUE_UUID, patientAUuid);
		
		Map<String, Object> patientB = new HashMap<>();
		patientB.put(FIELD_ACTIVE, true);
		patientB.put(FIELD_VALUE_UUID, patientBUuid);
		
		entryList.add(messageHeader);
		entryList.add(patientA);
		entryList.add(patientB);
		
		Map<String, Object> resourceData = new HashMap<>();
		Map<String, Object> resource = new HashMap<>();
		
		String resourceType = "Bundle";
		resource.put("resourceType", resourceType);
		patientB.put("resource", resource);
		patientA.put("resource", resource);
		resourceData.put("resourceType", resourceType);
		resourceData.put("resource", "");
		resourceData.put("entry", entryList);
		
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		
		String bundleData = resourceData.toString();
		doAnswer(invocation -> {
			Map<String, Object> bundleSubmissionResponse = resourceData;
			List<String> references = new ArrayList<>();
			references.add("Patient/" + patientA.get(FIELD_VALUE_UUID));
			references.add("Patient/" + patientB.get(FIELD_VALUE_UUID));
			
			bundleSubmissionResponse.put("reference", references);
			
			return bundleSubmissionResponse;
		}).when(mpiHttpClient).submitRequest("/fhiir-url/test", bundleData, List.class);
		
		Map<String, Object> bundleSubmissionResponse = (Map) mpiHttpClient.submitBundle("/fhiir-url/test", bundleData,
		    List.class);
		assertEquals(bundleSubmissionResponse.get("resourceType"), resourceType);
		assertEquals((bundleSubmissionResponse.get("entry")), resourceData.get("entry"));
		assertNotNull(bundleSubmissionResponse.get("reference"));
	}
	
	@Test
	public void submitPatient_shouldSubmitPatientAsSanteMpi() throws Exception {
		// Mock for HttpURLConnection
		when(MpiUtils.openConnection("sante.fake//fhiir-url/test")).thenReturn(httpURLConnectionMock);
		when(MpiUtils.openConnection("sante.fake/fhir/Patient")).thenReturn(httpURLConnectionMock);
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		
		// Mock inputSTream for mock connection
		TokenInfo tokenInfo = new TokenInfo();
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(2500, ChronoUnit.MILLIS));
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
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getTokenInfo()).thenReturn(tokenInfo);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AuthenticationType.OAUTH);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
		
		final String patientUuid = "patient-uuid-santempi";
		
		Map<String, Object> patientData = new HashMap<>();
		patientData.put(FIELD_ACTIVE, true);
		patientData.put(OPENMRS_UUID, patientUuid);
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		String patientDataAsString = objectMapper.writeValueAsString(patientData);
		
		doNothing().when(mpiHttpClient)
		        .doAuthentication("grant_type=refresh_token&refresh_token=REFRESH_TOKEN&client_secret=null&client_id=null");
		
		ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
		mpiHttpClient.submitPatient(patientDataAsString);
		verify(mpiHttpClient).submitPatient(argumentCaptor.capture());
		Map submitedData = objectMapper.readValue(argumentCaptor.getValue(), Map.class);
		assertEquals(patientData.get(FIELD_ACTIVE), submitedData.get(FIELD_ACTIVE));
		assertEquals(patientData.get(OPENMRS_UUID), submitedData.get(OPENMRS_UUID));
	}
	
	@Test
	public void submitPatient_shouldSubmitPatientAsOPenCr() throws Exception {
		// Mock for HttpURLConnection
		when(MpiUtils.openConnectionForSSL("sante.fake//fhiir-url/test", mpiContextMock)).thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("sante.fake/auth/oauth2_token", mpiContextMock))
		        .thenReturn(httpsURLConnectionMock);
		when(MpiUtils.openConnectionForSSL("sante.fake/fhir/Patient", mpiContextMock)).thenReturn(httpsURLConnectionMock);
		OutputStream outputStreamMock = PowerMockito.mock(OutputStream.class);
		when(httpsURLConnectionMock.getOutputStream()).thenReturn(outputStreamMock);
		when(httpsURLConnectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getServerBaseUrl()).thenReturn(MPI_BASE_URL);
		when(MpiContext.initIfNecessary()).thenReturn(mpiContextMock);
		when(mpiContextMock.getAuthenticationType()).thenReturn(AuthenticationType.CERTIFICATE);
		when(mpiContextMock.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);
		
		SSLContext sslContextMock = PowerMockito.mock(SSLContext.class);
		mpiContextMock.setSslContext(sslContextMock);
		when(mpiContextMock.getSslContext()).thenReturn(sslContextMock);
		
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
		
		String patientDataAsString = objectMapper.writeValueAsString(patientData);
		mpiHttpClient = Mockito.spy(mpiHttpClient);
		this.mpiHttpClient.submitPatient(patientDataAsString);
		
		ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
		verify(mpiHttpClient).submitPatient(argumentCaptor.capture());
		Map submitedData = objectMapper.readValue(argumentCaptor.getValue(), Map.class);
		assertEquals(patientData.get(FIELD_ACTIVE), submitedData.get(FIELD_ACTIVE));
		assertEquals(patientData.get(OPENMRS_UUID), submitedData.get(OPENMRS_UUID));
	}
}
