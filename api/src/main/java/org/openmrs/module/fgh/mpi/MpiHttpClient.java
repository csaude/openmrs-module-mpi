package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PASS;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PATH;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Http client that posts patient data to the MPI
 */
@Component("mpiHttpClient")
public class MpiHttpClient {
	
	private static final Logger log = LoggerFactory.getLogger(MpiHttpClient.class);
	
	private SSLContext sslContext;
	
	public void submitPatient(String patientData) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Received request to submit patient to MPI");
		}
		
		AdministrationService adminService = Context.getAdministrationService();
		
		synchronized (this) {
			//Add global property listener to rebuild the SSL context when the GP values change
			if (sslContext == null) {
				log.info("Setting up SSL context using configured client certificate");
				
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
			}
		}
		
		String serverBaseUrl = adminService.getGlobalProperty(GP_MPI_BASE_URL);
		if (StringUtils.isBlank(serverBaseUrl)) {
			throw new APIException(GP_MPI_BASE_URL + " global property value is not set");
		}
		
		if (log.isDebugEnabled()) {
			log.debug("OpenCR base server URL: " + serverBaseUrl);
		}
		
		HttpsURLConnection connection = (HttpsURLConnection) new URL(serverBaseUrl + "/fhir/Patient").openConnection();
		
		try {
			connection.setSSLSocketFactory(sslContext.getSocketFactory());
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			
			log.info("Submitting patient to MPI");
			
			connection.connect();
			OutputStream out = connection.getOutputStream();
			out.write(patientData.getBytes());
			out.flush();
			out.close();
			
			if (connection.getResponseCode() != 200) {
				final String error = connection.getResponseCode() + " " + connection.getResponseMessage();
				throw new APIException("Unexpected response " + error + " when submitting patient to MPI");
			}
			
			InputStream response = (InputStream) connection.getContent();
			if (log.isDebugEnabled()) {
				log.debug("Response: " + IOUtils.toString(response, StandardCharsets.UTF_8));
			}
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		
	}
	
}
