package org.openmrs.module.fgh.mpi.integ;

import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_AUTHENTICATION_TYPE;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_KEYSTORE_PASS;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_KEYSTORE_PATH;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_KEYSTORE_TYPE;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_MPI_APP_CONTENT_TYPE;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_MPI_SYSTEM;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_SANTE_CLIENT_ID;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_SANTE_CLIENT_SECRET;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.GP_UUID_SYSTEM;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fgh.mpi.entity.AuthenticationType;
import org.openmrs.module.fgh.mpi.entity.MpiSystemType;
import org.openmrs.module.fgh.mpi.entity.TokenInfo;
import org.openmrs.module.fgh.mpi.utils.FhirUtils;
import org.openmrs.module.fgh.mpi.utils.MpiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiContext {
	
	private static final Logger log = LoggerFactory.getLogger(MpiContext.class);
	
	private TokenInfo tokenInfo;
	
	private String contentType;
	
	private AuthenticationType authenticationType;
	
	private boolean contextInitialized;
	
	private SSLContext sslContext;
	
	private String serverBaseUrl;
	
	private MpiSystemType mpiSystem;
	
	private String openmrsUuidSystem;
	
	private String clientId;
	
	private String clientSecret;
	
	public static MpiContext mpiContext;
	
	public MpiContext() {
	}
	
	public static synchronized MpiContext initIfNecessary() throws Exception {
		if (mpiContext == null) {
			mpiContext = new MpiContext();
			
			mpiContext.init();
		}
		
		return mpiContext;
	}
	
	protected void init() throws Exception {
		
		//TODO: Add global property listener to rebuild the SSL context when the GP values change
		if (!this.contextInitialized) {
			AdministrationService adminService = Context.getAdministrationService();
			
			String gpAuthenticationType = adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE);
			
			if (StringUtils.isBlank(gpAuthenticationType)) {
				throw new APIException(GP_AUTHENTICATION_TYPE + " global property value is not set");
			}
			
			this.authenticationType = AuthenticationType.valueOf(gpAuthenticationType);
			
			if (log.isDebugEnabled()) {
				log.debug("Authentication Type: " + this.authenticationType);
			}
			
			this.serverBaseUrl = adminService.getGlobalProperty(GP_MPI_BASE_URL);
			if (StringUtils.isBlank(this.serverBaseUrl)) {
				throw new APIException(GP_MPI_BASE_URL + " global property value is not set");
			}
			
			if (log.isDebugEnabled()) {
				log.debug("The MPI Syetem base server URL: " + this.serverBaseUrl);
			}
			
			String gpMpiSystem = adminService.getGlobalProperty(GP_MPI_SYSTEM);
			
			if (StringUtils.isBlank(gpMpiSystem)) {
				throw new APIException(GP_MPI_SYSTEM + " global property value is not set");
			}
			
			this.mpiSystem = MpiSystemType.valueOf(gpMpiSystem);
			
			if (log.isDebugEnabled()) {
				log.debug("MPI system: " + gpMpiSystem);
			}
			
			this.openmrsUuidSystem = MpiUtils.getGlobalPropertyValue(GP_UUID_SYSTEM);
			
			if (StringUtils.isBlank(this.openmrsUuidSystem)) {
				throw new APIException(GP_UUID_SYSTEM + " global property value is not set");
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Openmrs UUID System: " + this.openmrsUuidSystem);
			}
			
			this.contentType = adminService.getGlobalProperty(GP_MPI_APP_CONTENT_TYPE);
			if (StringUtils.isBlank(this.serverBaseUrl)) {
				throw new APIException(GP_MPI_APP_CONTENT_TYPE + " global property value is not set");
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Content Type: " + this.contentType);
			}
			
			if (this.authenticationType.isCertificate()) {
				initSSL();
			} else if (this.authenticationType.isOuath()) {
				initOauth();
			}
			
			this.contextInitialized = true;
		}
	}
	
	protected void initOauth() {
		AdministrationService adminService = Context.getAdministrationService();
		
		this.clientId = adminService.getGlobalProperty(GP_SANTE_CLIENT_ID);
		
		if (StringUtils.isBlank(this.clientId)) {
			throw new APIException(GP_SANTE_CLIENT_ID + " global property value is not set");
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Client Id: " + this.clientId);
		}
		
		this.clientSecret = adminService.getGlobalProperty(GP_SANTE_CLIENT_SECRET);
		
		if (StringUtils.isBlank(this.clientSecret)) {
			throw new APIException(GP_SANTE_CLIENT_SECRET + " global property value is not set");
		}
	}
	
	protected void initSSL() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
	        FileNotFoundException, UnrecoverableKeyException, KeyManagementException {
		
		AdministrationService adminService = Context.getAdministrationService();
		
		log.info("Setting up SSL context using configured client certificate and MPI server base URL");
		
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
		
		KeyStore ks = FhirUtils.getKeyStoreInstanceByType(keyStoreType, keyStorePath, keyStorePassArray);
		
		KeyManagerFactory kmf = FhirUtils.getKeyManagerFactoryInstance("SunX509");
		kmf.init(ks, keyStorePassArray);
		this.sslContext = FhirUtils.getSslContextByProtocol("TLSv1.2");
		this.initSSLContext(this.sslContext, kmf);
		
		//We are communicating wih our own service that uses a self signed certificate so no need for
		//host name verification
		HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
	}
	
	protected void initSSLContext(SSLContext sslContext, KeyManagerFactory keyManagerFactory) throws KeyManagementException {
		sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public AuthenticationType getAuthenticationType() {
		return authenticationType;
	}
	
	public void setAuthenticationType(AuthenticationType authenticationType) {
		this.authenticationType = authenticationType;
	}
	
	public boolean isContextInitialized() {
		return contextInitialized;
	}
	
	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}
	
	public SSLContext getSslContext() {
		return sslContext;
	}
	
	public String getServerBaseUrl() {
		return serverBaseUrl;
	}
	
	public void setServerBaseUrl(String serverBaseUrl) {
		this.serverBaseUrl = serverBaseUrl;
	}
	
	public MpiSystemType getMpiSystem() {
		return mpiSystem;
	}
	
	public String getOpenmrsUuidSystem() {
		return openmrsUuidSystem;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public String getClientSecret() {
		return clientSecret;
	}
	
	public TokenInfo getTokenInfo() {
		return tokenInfo;
	}
	
	public void initToken(TokenInfo tokenInfo) {
		this.tokenInfo = tokenInfo;
		this.tokenInfo
		        .setTokenExpirationDateTime(LocalDateTime.now().plus(this.tokenInfo.getExpiresIn(), ChronoUnit.MILLIS));
	}
}
