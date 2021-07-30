package org.openmrs.module.fgh.mpi;

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
	
	public void submitPatient(String patientData) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("In MPI Http Client");
		}
		
		AdministrationService adminService = Context.getAdministrationService();
		String keyStorePath = adminService.getGlobalProperty(MpiConstants.GP_KEYSTORE_PATH);
		String keyStorePass = adminService.getGlobalProperty(MpiConstants.GP_KEYSTORE_PASS);
		String keyStoreType = adminService.getGlobalProperty(MpiConstants.GP_KEYSTORE_TYPE);
		char[] keyStorePassArray = null;
		if (StringUtils.isBlank(keyStorePass)) {
			keyStorePassArray = keyStorePass.toCharArray();
		}
		
		KeyStore ks = KeyStore.getInstance(keyStoreType);
		ks.load(new FileInputStream(keyStorePath), keyStorePassArray);
		
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, keyStorePassArray);
		SSLContext sc = SSLContext.getInstance("TLSv1.2");
		sc.init(kmf.getKeyManagers(), null, new SecureRandom());
		
		String serverBaseUrl = adminService.getGlobalProperty(MpiConstants.GP_MPI_BASE_URL);
		HttpsURLConnection connection = (HttpsURLConnection) new URL(serverBaseUrl + "/fhir/Patient").openConnection();
		
		try {
			connection.setSSLSocketFactory(sc.getSocketFactory());
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
