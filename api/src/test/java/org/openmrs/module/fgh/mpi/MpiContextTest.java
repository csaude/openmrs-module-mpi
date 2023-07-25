package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_AUTHENTICATION_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PASS;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_PATH;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_KEYSTORE_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_APP_CONTENT_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_SECRET;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_EVENT_URI;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, FhirUtils.class, MpiContext.class, BaseEventProcessor.class,
        KeyManagerFactory.class })
public class MpiContextTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private SnapshotEventProcessor snapshotEventProcessor;
	
	@Mock
	private AdministrationService adminService;
	
	private static final String UUID_SYSTEM = "http://test.openmrs.id/uuid";
	
	private static final String MESSAGE_HEADER_REFERENCE = "metadata.epts.e-saude.net/bundle";
	
	private static final String MESSAGE_HEADER_EVENT_URI = "urn:ihe:iti:pmir:2019:patient-feed";
	
	private static final AuthenticationType AUTHENTICATION_TYPE = AuthenticationType.OAUTH;
	
	private static final String MPI_BASE_URL = "http://sante.org.mz";
	
	private static final String MPI_APP_CONTENT_TYPE = "application/fhir+json";
	
	private static final MpiSystemType MPI_SYSTEM = MpiSystemType.SANTEMPI;
	
	private static final MpiSystemType MPI_SYSTEM_AS_OPENCR = MpiSystemType.SANTEMPI;
	
	private static final String SANTE_CLIENT_ID = "client_credentials";
	
	private static final String SANTE_CLIENT_SECRET = "bG6TuS3X-H1MsT4ctW!CxXjK9J4l1QpK8B0Q";
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(FhirUtils.class);
		PowerMockito.mockStatic(MpiContext.class);
		PowerMockito.mockStatic(KeyStore.class);
		PowerMockito.mockStatic(KeyManagerFactory.class);
		Whitebox.setInternalState(snapshotEventProcessor, MpiHttpClient.class, mockMpiHttpClient);
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
	}
	
	@Test
	public void init_shouldInitOauth() throws Exception {
		AuthenticationType OUAUTH = AuthenticationType.OAUTH;
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(OUAUTH.toString());
		when(adminService.getGlobalProperty(GP_MPI_SYSTEM)).thenReturn(MPI_SYSTEM.toString());
		MpiContext initOauthContext = new MpiContext();
		initOauthContext.init();
		
		assertEquals(OUAUTH, initOauthContext.getAuthenticationType());
		assertEquals(MPI_BASE_URL, initOauthContext.getServerBaseUrl());
		assertEquals(MPI_SYSTEM, initOauthContext.getMpiSystem());
		assertEquals(UUID_SYSTEM, initOauthContext.getOpenmrsUuidSystem());
		assertEquals(SANTE_CLIENT_ID, initOauthContext.getClientId());
		assertEquals(SANTE_CLIENT_SECRET, initOauthContext.getClientSecret());
		assertTrue(initOauthContext.isContextInitialized());
	}
	
	@Test
	public void init_shouldInitSSL() throws Exception {
		KeyStore keyStoreMock = PowerMockito.mock(KeyStore.class);
		KeyManagerFactory keyManagerFactoryMock = PowerMockito.mock(KeyManagerFactory.class);
		SSLContext sslContextMock = PowerMockito.mock(SSLContext.class);
		
		AuthenticationType CERTIFICATE = AuthenticationType.CERTIFICATE;
		when(adminService.getGlobalProperty(GP_AUTHENTICATION_TYPE)).thenReturn(CERTIFICATE.toString());
		when(adminService.getGlobalProperty(GP_MPI_SYSTEM)).thenReturn(MPI_SYSTEM_AS_OPENCR.toString());
		when(adminService.getGlobalProperty(GP_KEYSTORE_PATH)).thenReturn("src/test/resources/test/test-resource.xml");
		when(adminService.getGlobalProperty(GP_KEYSTORE_PASS)).thenReturn(GP_KEYSTORE_PASS);
		when(adminService.getGlobalProperty(GP_KEYSTORE_TYPE)).thenReturn(GP_KEYSTORE_TYPE);
		when(FhirUtils.getKeyStoreInstanceByType(GP_KEYSTORE_TYPE)).thenReturn(keyStoreMock);
		when(FhirUtils.getKeyManagerFactoryInstance("SunX509")).thenReturn(keyManagerFactoryMock);
		when(FhirUtils.getSslContextByProtocol("TLSv1.2")).thenReturn(sslContextMock);
		//doNothing().when(sslContextMock).init(null, null, new SecureRandom());
		MpiContext initSSLContext = new MpiContext();
		initSSLContext.init();
		
		assertEquals(CERTIFICATE, initSSLContext.getAuthenticationType());
		assertEquals(MPI_BASE_URL, initSSLContext.getServerBaseUrl());
		assertEquals(MPI_SYSTEM_AS_OPENCR, initSSLContext.getMpiSystem());
		assertEquals(UUID_SYSTEM, initSSLContext.getOpenmrsUuidSystem());
		assertFalse(initSSLContext.isContextInitialized());
	}
}
