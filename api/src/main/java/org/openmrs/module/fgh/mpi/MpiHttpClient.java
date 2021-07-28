package org.openmrs.module.fgh.mpi;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Http client that posts patient data to the MPI
 */
@Component("mpiHttpClient")
public class MpiHttpClient {
	
	private static final Logger log = LoggerFactory.getLogger(MpiHttpClient.class);
	
	public void postPatient(String patientData) throws Exception {
		CloseableHttpClient httpClient = null;
		
		try {
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new File(""), "".toCharArray()).build();
			SSLConnectionSocketFactory sslContextFactory = new SSLConnectionSocketFactory(sslContext);
			httpClient = HttpClients.custom()//.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			        .setSSLSocketFactory(sslContextFactory).build();
			HttpPost httpPost = new HttpPost("");
			httpPost.setEntity(new StringEntity(patientData, ContentType.APPLICATION_JSON));
			
			CloseableHttpResponse r = httpClient.execute(httpPost);
		}
		finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				}
				catch (IOException e) {
					log.warn("Failed to close http client instance");
				}
			}
		}
	}
	
}
