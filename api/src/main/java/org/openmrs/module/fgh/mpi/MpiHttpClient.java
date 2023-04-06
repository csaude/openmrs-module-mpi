package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiConstants.AUTHENTICATION_OUTH_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.AUTHENTICATION_SSL_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_AUTHENTICATION_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PASS;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PATH;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_APP_CONTENT_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_SECRET;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_SCOPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.HTTP_REQUEST_SUCCESS_RANGE;
import static org.openmrs.module.fgh.mpi.MpiConstants.REQ_PARAM_SOURCE_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.RESPONSE_FIELD_PARAM;
import static org.openmrs.module.fgh.mpi.MpiConstants.RESPONSE_FIELD_VALUE_REF;
import static org.openmrs.module.fgh.mpi.MpiUtils.getGlobalPropertyValue;
import static org.openmrs.module.fgh.mpi.MpiUtils.isOpenCrMPI;
import static org.openmrs.module.fgh.mpi.MpiUtils.isSanteMPI;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fgh.mpi.model.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Http client that posts patient data to the MPI
 */
@Component("mpiHttpClient")
public class MpiHttpClient {
	
	private static final Logger log = LoggerFactory.getLogger(MpiHttpClient.class);
	
	public final static String CONTENT_TYPE = "application/json";
	
	private SSLContext sslContext;
	
	private String serverBaseUrl;
	
	private static final String SUBPATH_FHIR = "fhir";
	
	private static final String SUBPATH_PATIENT = SUBPATH_FHIR + "/Patient";
	
	private static String openmrsUuidSystem;
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private TokenInfo tokenInfo;
	
	/**
	 * Looks up the patient with the specified OpenMRS uuid from the MPI
	 *
	 * @param patientUuid the patient's OpenMRS uuid
	 * @return map representation of the patient fhir resource
	 * @throws Exception
	 */
	public Map<String, Object> getPatient(String patientUuid) throws Exception {
		log.info("Looking up patient record from MPI with OpenMRS uuid: " + patientUuid);
		
		if (log.isDebugEnabled()) {
			log.debug("Searching for patient from MPI with OpenMRS uuid: " + patientUuid);
		}
		
		String mpiSystem = getGlobalPropertyValue(GP_MPI_SYSTEM);
		
		if (openmrsUuidSystem == null) {
			synchronized (MpiHttpClient.class) {
				openmrsUuidSystem = MpiUtils.getGlobalPropertyValue(GP_UUID_SYSTEM);
			}
		}
		
		String query = REQ_PARAM_SOURCE_ID + "=" + openmrsUuidSystem + "|" + patientUuid;
		
		Map<String, Object> pixResponse = null;
		
		try {
			pixResponse = submitRequest(SUBPATH_PATIENT + "/$ihe-pix?" + query, null, Map.class);
		}
		catch (APIException e) {
			//When a queried patient is not present in Sante DB it throws an Exception.
			//This actualy is not any error,so bellow the exception is caught 
			if (e.getLocalizedMessage().contains("404") && isSanteMPI(mpiSystem)) {
				pixResponse = new HashMap<String, Object>();
			} else
				throw e;
		}
		
		List<Map<String, Object>> ids = (List<Map<String, Object>>) pixResponse.get(RESPONSE_FIELD_PARAM);
		if (ids == null || ids.isEmpty()) {
			return null;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Fetching actual patient record from MPI");
		}
		
		String remoteRef = null;
		
		if (isSanteMPI(mpiSystem)) {
			remoteRef = (ids.get(ids.size() - 1).get(RESPONSE_FIELD_VALUE_REF).toString().split("/")[1]).split(",")[0];
		} else if (isOpenCrMPI(mpiSystem)) {
			String[] patientUrlParts = ids.get(0).get(RESPONSE_FIELD_VALUE_REF).toString().split("/");
			
			remoteRef = patientUrlParts[patientUrlParts.length - 1];
		} else
			throw new APIException("Unsupported MPI System!!! [" + mpiSystem + "]");
		
		return submitRequest(SUBPATH_PATIENT + "/" + remoteRef, null, Map.class);
		
	}
	
	/**
	 * Submits the specified bundle data to the MPI
	 *
	 * @param bundleData the bundle fhir json payload
	 * @throws Exception
	 */
	public <T> T submitBundle(String fhirURL, String bundleData, Class<T> responseType) throws Exception {
		log.info("Submitting patient bundle to the MPI");
		
		T response = submitRequest(fhirURL, bundleData, responseType);
		
		if (log.isDebugEnabled()) {
			log.debug("MPI patient bundle submission response: " + response);
		}
		
		log.info("Successfully submitted the patient bundle to the MPI");
		
		return response;
	}
	
	/**
	 * Submits the specified patient data to the MPI
	 *
	 * @param patientData the patient fhir json payload
	 * @throws Exception
	 */
	public void submitPatient(String patientData) throws Exception {
		log.info("Submitting patient record to the MPI");
		if (log.isDebugEnabled()) {
			log.debug("Patient data -> " + patientData);
		}
		
		AdministrationService adminService = Context.getAdministrationService();
		
		String mpiSystem = adminService.getGlobalProperty(GP_MPI_SYSTEM);
		
		if (MpiUtils.isSanteMPI(mpiSystem)) {
			Map<String, Object> santeResponse = submitRequest(SUBPATH_PATIENT, patientData, Map.class);
			
			if (log.isDebugEnabled()) {
				log.debug("MPI patient submission response: " + santeResponse);
			}
			
			if (santeResponse == null || santeResponse.isEmpty()) {
				throw new APIException("An empty response was received when the patient was submitted");
			}
		} else {
			List<Map<String, Object>> mpiIdsResp = submitRequest(SUBPATH_PATIENT, patientData, List.class);
			
			if (log.isDebugEnabled()) {
				log.debug("MPI patient submission response: " + mpiIdsResp);
			}
			
			if (CollectionUtils.isEmpty(mpiIdsResp) || mpiIdsResp.get(0) == null) {
				throw new APIException("An empty response was received when the patient was submitted");
			}
		}
		
		log.info("Successfully submitted the patient record to the MPI");
	}
	
	private <T> void retriveAccessToken(Class<T> responseType) throws MalformedURLException, IOException {
		
		AdministrationService adminService = Context.getAdministrationService();
		
		String clientId = adminService.getGlobalProperty(GP_SANTE_CLIENT_ID);
		String clientSecret = adminService.getGlobalProperty(GP_SANTE_CLIENT_SECRET);
		//String loginType = adminService.getGlobalProperty(GP_SANTE_LOGIN_TYPE);
		String scope = adminService.getGlobalProperty(GP_SANTE_SCOPE);
		
		String data = "grant_type=" + loginType + "&" + "scope=" + scope + "&" + "client_secret=" + clientSecret + "&"
		        + "client_id=" + clientId;
		
		// Request a Refresh token in case it expires 
		if (this.tokenInfo != null) {
			
			if (!this.tokenInfo.isValid()) {
				//Implement a refresh token method
				data = "grant_type=refresh_token&refresh_token=" + this.tokenInfo.getRefresh_token() + "&" + "client_secret="
				        + clientSecret + "&" + "client_id=" + clientId;
				this.doAuthentication(data);
			}
		} else {
			// Normal Login 
			this.doAuthentication(data);
		}
	}
	
	private void doAuthentication(String data) throws MalformedURLException, IOException {
		AdministrationService adminService = Context.getAdministrationService();
		
		String uri = "/auth/oauth2_token";
		this.serverBaseUrl = adminService.getGlobalProperty(GP_MPI_BASE_URL);
		String url = serverBaseUrl + uri;
		
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		
		Range<Integer> successRange = Range.between(200, 299);
		
		try {
			connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));
			
			connection.setConnectTimeout(30000);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			
			connection.connect();
			
			OutputStream out = connection.getOutputStream();
			out.write(data.getBytes());
			out.flush();
			out.close();
			
			if (!successRange.contains(connection.getResponseCode())) {
				final String error = connection.getResponseCode() + " " + connection.getResponseMessage();
				throw new APIException("Unexpected response " + error + " from MPI");
			}
			
			this.tokenInfo = MAPPER.readValue(connection.getInputStream(), TokenInfo.class);
			this.tokenInfo.timeCountDown();
		}
		catch (Exception e) {
			e.printStackTrace();
			
			throw e;
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	/**
	 * Submits a request to the MPI
	 *
	 * @param requestPath the string to append to the URL
	 * @param data the data to post if any
	 * @param responseType the type of response to return
	 * @param <T>
	 * @return the response from the MPI
	 * @throws Exception
	 */
	private <T> T submitRequest(String requestPath, String data, Class<T> responseType) throws Exception {
		AdministrationService adminService = Context.getAdministrationService();
		
		serverBaseUrl = adminService.getGlobalProperty(GP_MPI_BASE_URL);
		String url = serverBaseUrl + "/" + requestPath;
		
		String contentType = adminService.getGlobalProperty(GP_MPI_APP_CONTENT_TYPE);
		String authenticationType = adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE);
		
		HttpURLConnection connection = null;
		
		if (authenticationType.equals(AUTHENTICATION_OUTH_TYPE)) {
			retriveAccessToken(responseType);
			
			connection = (HttpURLConnection) new URL(url).openConnection();
			
			String authHeaderValue = "bearer " + this.tokenInfo.getAccess_token();
			
			connection.setRequestProperty("Authorization", authHeaderValue);
		} else if (authenticationType.equals(AUTHENTICATION_SSL_TYPE)) {
			initIfNecessary();
			
			connection = (HttpsURLConnection) new URL(url).openConnection();
			
			((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
			
		} else {
			throw new APIException("Unsupported Authentication type");
		}
		
		try {
			connection.setRequestProperty("Accept", contentType);
			connection.setDoInput(true);
			connection.setConnectTimeout(30000);
			connection.setUseCaches(false);
			
			if (data != null) {
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", contentType);
				connection.setDoOutput(true);
			} else {
				connection.setRequestMethod("GET");
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Making http request to MPI");
			}
			
			connection.connect();
			
			if (data != null) {
				OutputStream out = connection.getOutputStream();
				out.write(data.getBytes());
				out.flush();
				out.close();
			}
			
			if (!HTTP_REQUEST_SUCCESS_RANGE.contains(connection.getResponseCode())) {
				final String error = connection.getResponseCode() + " " + connection.getResponseMessage();
				
				throw new APIException("Unexpected response " + error + " from MPI");
			}
			
			return MAPPER.readValue(connection.getInputStream(), responseType);
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	private void initIfNecessary() throws Exception {
		
		synchronized (this) {
			//Add global property listener to rebuild the SSL contexte when the GP values change
			if (sslContext == null || serverBaseUrl == null) {
				log.info("Setting up SSL context using configured client certificate and MPI server base URL");
				AdministrationService adminService = Context.getAdministrationService();
				
				String keyStorePath = adminService.getGlobalProperty(GP_KEYSTORE_PATH);
				if (StringUtils.isBlank(keyStorePath)) {
					throw new APIException(GP_KEYSTORE_PATH + " global property value is not set");
				}
				
				String keyStorePass = adminService.getGlobalProperty(GP_KEYSTORE_PASS);
				char[] keyStorePassArray = "".toCharArray();
				if (keyStorePass != null) {
					keyStorePassArray = keyStorePass.toCharArray();
				}
				
				String keyStoreType = adminService.getGlobalProperty(GP_KEYSTORE_TYPE);
				if (StringUtils.isBlank(keyStoreType)) {
					throw new APIException(GP_KEYSTORE_TYPE + " global property value is not set");
				}
				
				log.info("Keystore path: " + keyStorePath);
				log.info("Keystore Type: " + keyStoreType);
				
				KeyStore ks = KeyStore.getInstance(keyStoreType);
				ks.load(new FileInputStream(keyStorePath), keyStorePassArray);
				
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				kmf.init(ks, keyStorePassArray);
				sslContext = SSLContext.getInstance("TLSv1.2");
				sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
				
				//We are communicating wih our own service that uses a self signed certificate so no need for
				//host name verification
				HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
				
				serverBaseUrl = adminService.getGlobalProperty(GP_MPI_BASE_URL);
				if (StringUtils.isBlank(serverBaseUrl)) {
					throw new APIException(GP_MPI_BASE_URL + " global property value is not set");
				}
				
				if (log.isDebugEnabled()) {
					log.debug("OpenCR base server URL: " + serverBaseUrl);
				}
			}
		}
	}
}
