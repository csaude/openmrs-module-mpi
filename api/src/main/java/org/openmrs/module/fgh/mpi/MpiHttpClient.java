package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PASS;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PATH;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;

/**
 * Http client that posts patient data to the MPI
 */
@Component("mpiHttpClient")
public class MpiHttpClient {
	
	private static final Logger log = LoggerFactory.getLogger(MpiHttpClient.class);
	
	public final static String CONTENT_TYPE = "application/json";
	
	private SSLContext sslContext;
	
	private String serverBaseUrl;
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	/**
	 * Looks up the patient with the specified OpenMRS uuid from the MPI
	 * 
	 * @param patientUuid the patient's OpenMRS uuid
	 * @return map representation of the patient fhir resource
	 * @throws Exception
	 */
	public Map<String, Object> getPatient(String patientUuid) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Received request to fetch patient from MPI with OpenMRS uuid -> " + patientUuid);
		}
		
		//TODO possibly query OpenCR after https://github.com/intrahealth/client-registry/issues/65 is resolved
		//return submitRequest("/fhir/Patient/" + mpiUuid, null, Map.class);
		
		return getPatientFromHapiFhir(patientUuid);
	}
	
	/**
	 * Submit the specified patient data to the MPI
	 * 
	 * @param patientData the patient fhir json payload
	 * @return a map representation of the created patient's MPI uuids
	 * @throws Exception
	 */
	public List<Map<String, Object>> submitPatient(String patientData) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Received request to submit patient to MPI");
		}
		
		return submitRequest("/fhir/Patient", patientData, List.class);
	}
	
	/**
	 * Submits a request to the MPI
	 * 
	 * @param urlPath request path to submit to
	 * @param data the data to post if any
	 * @param responseType the type of response to return
	 * @param <T>
	 * @return the response from the MPI
	 * @throws Exception
	 */
	private <T> T submitRequest(String urlPath, String data, Class<T> responseType) throws Exception {
		initIfNecessary();
		HttpsURLConnection connection = (HttpsURLConnection) new URL(serverBaseUrl + urlPath).openConnection();
		
		try {
			connection.setSSLSocketFactory(sslContext.getSocketFactory());
			connection.setRequestProperty("Accept", CONTENT_TYPE);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			
			if (data != null) {
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", CONTENT_TYPE);
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
			
			if (connection.getResponseCode() != 200) {
				final String error = connection.getResponseCode() + " " + connection.getResponseMessage();
				throw new APIException("Unexpected response " + error + " from MPI");
			}
			
			return MAPPER.readValue((InputStream) connection.getContent(), responseType);
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
	
	/**
	 * Fetches the patient resource from the hapi fhir server
	 *
	 * @param patientUuid the uuid of the patient
	 * @return a map of the patient resource
	 */
	private Map<String, Object> getPatientFromHapiFhir(String patientUuid) throws Exception {
		FhirContext ctx = FhirContext.forR4();
		//TODO cache the hapi fhir url
		String hapiFhirUrl = Context.getAdministrationService().getGlobalProperty(MpiConstants.GP_HAPI_FHIR_BASE_URL);
		IGenericClient client = ctx.newRestfulGenericClient(hapiFhirUrl + "/fhir");
		ICriterion<?> icrit = Patient.IDENTIFIER.exactly().systemAndValues(MpiConstants.SYSTEM_SOURCE_ID, patientUuid);
		Bundle bundle = client.search().forResource(Patient.class).where(icrit).returnBundle(Bundle.class).encodedJson()
		        .execute();
		if (bundle.isEmpty() || bundle.getEntry().isEmpty()) {
			return null;
		}
		
		if (bundle.getEntry().size() > 1) {
			throw new APIException("Found multiple patients with the same OpenMRS uuid in the MPI");
		}
		
		String json = ctx.newJsonParser().encodeResourceToString(bundle.getEntry().get(0).getResource());
		
		return MAPPER.readValue(json, Map.class);
	}
	
}
