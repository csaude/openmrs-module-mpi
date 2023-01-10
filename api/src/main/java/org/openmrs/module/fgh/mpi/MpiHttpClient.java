package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PASS;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PATH;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.REQ_PARAM_SOURCE_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.RESPONSE_FIELD_PARAM;
import static org.openmrs.module.fgh.mpi.MpiConstants.RESPONSE_FIELD_VALUE_REF;

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
		
		if (openmrsUuidSystem == null) {
			synchronized (MpiHttpClient.class) {
				openmrsUuidSystem = MpiUtils.getGlobalPropertyValue(GP_UUID_SYSTEM);
			}
		}
		
		String query = REQ_PARAM_SOURCE_ID + "=" + openmrsUuidSystem + "|" + patientUuid;
		Map<String, Object> pixResponse = submitRequest(SUBPATH_PATIENT + "/$ihe-pix?" + query, null, Map.class);
		List<Map<String, Object>> ids = (List<Map<String, Object>>) pixResponse.get(RESPONSE_FIELD_PARAM);
		if (ids == null || ids.isEmpty()) {
			return null;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Fetching actual patient record from MPI");
		}
		
		String remoteRef = getTryToGetRemotePatientRefOnOpenSanteDB(ids);
		
		if (remoteRef == null)
			remoteRef = getTryToGetRemotePatientRefOnOpenCR(ids);
		
		return submitRequest(SUBPATH_PATIENT + "/" + remoteRef, null, Map.class);
	}
	
	private String getTryToGetRemotePatientRefOnOpenCR(List<Map<String, Object>> ids) {
		String[] patientUrlParts = ids.get(0).get(RESPONSE_FIELD_VALUE_REF).toString().split("/");
		
		return patientUrlParts[patientUrlParts.length - 1];
	}
	
	private String getTryToGetRemotePatientRefOnOpenSanteDB(List<Map<String, Object>> ids) {
		return (ids.get(ids.size() - 1).get(RESPONSE_FIELD_VALUE_REF).toString().split("/")[1]).split(",")[0];
	}
	
	/**
	 * Submits the specified bundle data to the MPI
	 *
	 * @param bundleData the bundle fhir json payload
	 * @throws Exception
	 */
	public List<Object> submitBundle(String bundleData) throws Exception {
		log.info("Submitting patient bundle to the MPI");
		
		List<Object> response = submitRequest(SUBPATH_FHIR, bundleData, List.class);
		
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
		
		List<Map<String, Object>> mpiIdsResp = submitRequest(SUBPATH_PATIENT, patientData, List.class);
		
		if (log.isDebugEnabled()) {
			log.debug("MPI patient submission response: " + mpiIdsResp);
		}
		
		if (CollectionUtils.isEmpty(mpiIdsResp) || mpiIdsResp.get(0) == null) {
			throw new APIException("An empty response was received when the patient was submitted");
		}
		
		log.info("Successfully submitted the patient record to the MPI");
	}
	
	private <T> void retriveAccessToken(Class<T> responseType) throws MalformedURLException, IOException {
		if (this.tokenInfo != null && this.tokenInfo.isValid()) {
			return;
		}
		
		String url = "http://10.10.2.2:8080/auth/oauth2_token";
		
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		
		String data = "grant_type=client_credentials&scope=*&client_secret=8p0AlC0m~A0KsK4V5J~s5w4W5J8T8Q7i2N2I&client_id=openmrs_mpi_testing";
		
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
			
			if (connection.getResponseCode() != 200) {
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
		retriveAccessToken(responseType);
		
		//initIfNecessary();
		String url = "http://10.10.2.2:8080" + "/" + requestPath;
		//HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
		
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		
		String authHeaderValue = "bearer " + this.tokenInfo.getAccess_token();
		
		try {
			connection.setRequestProperty("Authorization", authHeaderValue);
			connection.setRequestProperty("Accept", "application/fhir+json");
			connection.setDoInput(true);
			connection.setConnectTimeout(30000);
			connection.setUseCaches(false);
			
			if (data != null) {
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/fhir+json");
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
			
			if (connection.getResponseCode() == 404) {
				return (T) new HashMap<String, Object>();
			} else if (connection.getResponseCode() != 200) {
				final String error = connection.getResponseCode() + " " + connection.getResponseMessage();
				
				throw new APIException("Unexpected response " + error + " from MPI");
			}
			
			return MAPPER.readValue(connection.getInputStream(), responseType);
		}
		catch (Exception e) {
			e.printStackTrace();
			
			return (T) new HashMap<String, Object>();
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	private void initIfNecessary() throws Exception {
		
		synchronized (this) {
			//Add global property listener to rebuild the SSL context when the GP values change
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
