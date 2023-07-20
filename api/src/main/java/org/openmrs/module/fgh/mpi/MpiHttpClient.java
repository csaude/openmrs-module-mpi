package org.openmrs.module.fgh.mpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.Range;
import org.openmrs.api.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.openmrs.module.fgh.mpi.MpiConstants.*;

/**
 * Http client that posts patient data to the MPI
 */
@Component("mpiHttpClient")
public class MpiHttpClient {
	
	private static final Logger log = LoggerFactory.getLogger(MpiHttpClient.class);
	
	private static final String SUBPATH_FHIR = "fhir";
	
	private static final String SUBPATH_PATIENT = SUBPATH_FHIR + "/Patient";
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	/**
	 * Looks up the patient with the specified OpenMRS uuid from the MPI
	 *
	 * @param patientUuid the patient's OpenMRS uuid
	 * @return map representation of the patient fhir resource
	 * @throws Exception
	 */
	public Map<String, Object> getPatient(String patientUuid) throws Exception {
		MpiContext mpiContext = MpiContext.initIfNecessary();
		
		log.info("Looking up patient record from MPI with OpenMRS uuid: " + patientUuid);
		
		if (log.isDebugEnabled()) {
			log.debug("Searching for patient from MPI with OpenMRS uuid: " + patientUuid);
		}
		
		String query = REQ_PARAM_SOURCE_ID + "=" + mpiContext.getOpenmrsUuidSystem() + "|" + patientUuid;
		
		Map<String, Object> pixResponse = null;
		
		pixResponse = submitRequest(SUBPATH_PATIENT + "/$ihe-pix?" + query, null, Map.class);
		
		List<Map<String, Object>> ids = (List<Map<String, Object>>) pixResponse.get(RESPONSE_FIELD_PARAM);
		if (CollectionUtils.isEmpty(ids)) {
			return null;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Fetching actual patient record from MPI");
		}
		
		String remoteRef = null;
		
		if (mpiContext.getMpiSystem().isSanteMPI()) {
			remoteRef = (ids.get(ids.size() - 1).get(RESPONSE_FIELD_VALUE_REF).toString().split("/")[1]).split(",")[0];
		} else if (mpiContext.getMpiSystem().isOpenCr()) {
			String[] patientUrlParts = ids.get(0).get(RESPONSE_FIELD_VALUE_REF).toString().split("/");
			
			remoteRef = patientUrlParts[patientUrlParts.length - 1];
		} else
			throw new APIException("Unsupported MPI System!!! [" + mpiContext.getMpiSystem() + "]");
		
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
		MpiContext mpiContext = MpiContext.initIfNecessary();
		
		log.info("Submitting patient record to the MPI");
		if (log.isDebugEnabled()) {
			log.debug("Patient data -> " + patientData);
		}
		
		if (mpiContext.getMpiSystem().isSanteMPI()) {
			Map<String, Object> santeResponse = submitRequest(SUBPATH_PATIENT, patientData, Map.class);
			
			if (log.isDebugEnabled()) {
				log.debug("MPI patient submission response: " + santeResponse);
			}
			
			if (MapUtils.isEmpty(santeResponse)) {
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
	
	private <T> void retriveAccessToken(Class<T> responseType) throws Exception {
		MpiContext mpiContext = MpiContext.initIfNecessary();
		
		String data = "grant_type=client_credentials" + "&" + "scope=*" + "&" + "client_secret="
		        + mpiContext.getClientSecret() + "&" + "client_id=" + mpiContext.getClientId();
		
		// Request a Refresh token in case it expires 
		if (mpiContext.getTokenInfo() != null) {
			
			if (!mpiContext.getTokenInfo().isValid()) {
				//Implement a refresh token method
				data = "grant_type=refresh_token&refresh_token=" + mpiContext.getTokenInfo().getRefreshToken() + "&"
				        + "client_secret=" + mpiContext.getClientSecret() + "&" + "client_id=" + mpiContext.getClientId();
				this.doAuthentication(data);
			}
		} else {
			// Normal Login 
			this.doAuthentication(data);
		}
	}
	
	protected void doAuthentication(String data) throws Exception {
		MpiContext mpiContext = MpiContext.initIfNecessary();
		
		String uri = "/auth/oauth2_token";
		String url = mpiContext.getServerBaseUrl() + uri;
		
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
			
			mpiContext.initToken(MAPPER.readValue(connection.getInputStream(), TokenInfo.class));
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
	protected <T> T submitRequest(String requestPath, String data, Class<T> responseType) throws Exception {
		MpiContext mpiContext = MpiContext.initIfNecessary();
		String url = mpiContext.getServerBaseUrl() + "/" + requestPath;
		int responseCode = 0;
		
		HttpURLConnection connection = null;
		
		if (mpiContext.getAuthenticationType().isOuath()) {
			retriveAccessToken(responseType);
			
			connection = (HttpURLConnection) new URL(url).openConnection();
			
			String authHeaderValue = "bearer " + mpiContext.getTokenInfo().getAccessToken();
			
			connection.setRequestProperty("Authorization", authHeaderValue);
		} else if (mpiContext.getAuthenticationType().isCertificate()) {
			
			connection = (HttpsURLConnection) new URL(url).openConnection();
			
			((HttpsURLConnection) connection).setSSLSocketFactory(mpiContext.getSslContext().getSocketFactory());
			
		} else {
			throw new APIException("Unsupported Authentication type");
		}
		
		try {
			connection.setRequestProperty("Accept", mpiContext.getContentType());
			connection.setDoInput(true);
			connection.setConnectTimeout(30000);
			connection.setUseCaches(false);
			
			if (data != null) {
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", mpiContext.getContentType());
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
			
			responseCode = connection.getResponseCode();
			
			if (mpiContext.getMpiSystem().isSanteMPI()) {
				if (!HTTP_REQUEST_SUCCESS_RANGE.contains(responseCode)) {
					if (responseCode == 404) {
						return (T) MapUtils.EMPTY_MAP;
					}
					handleUnexpectedResponse(responseCode, connection.getResponseMessage());
				}
			} else if (mpiContext.getMpiSystem().isOpenCr()) {
				if (responseCode != 200) {
					handleUnexpectedResponse(responseCode, connection.getResponseMessage());
				}
			}
			return MAPPER.readValue(connection.getInputStream(), responseType);
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	private void handleUnexpectedResponse(int responseCode, String responseMessage) {
		String error = responseCode + " " + responseMessage;
		throw new APIException("Unexpected response " + error + " from MPI");
	}
}
