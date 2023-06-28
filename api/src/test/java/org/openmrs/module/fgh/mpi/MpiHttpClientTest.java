package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_AUTHENTICATION_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_APP_CONTENT_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_BASE_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_MPI_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_CLIENT_SECRET;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_EVENT_URI;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class })
public class MpiHttpClientTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private MpiContext mpiContext;
	
	@Mock
	private AdministrationService adminService;
	
	private static final String UUID_SYSTEM = "http://test.openmrs.id/uuid";
	
	private static final String MESSAGE_HEADER_REFERENCE = "metadata.epts.e-saude.net/bundle";
	
	private static final String MESSAGE_HEADER_EVENT_URI = "urn:ihe:iti:pmir:2019:patient-feed";
	
	private static final AuthenticationType AUTHENTICATION_TYPE = AuthenticationType.OAUTH;
	
	private static final String MPI_BASE_URL = "http://sante.org.mz";
	
	private static final String MPI_APP_CONTENT_TYPE = "application/fhir+json";
	
	private static final MpiSystemType MPI_SYSTEM = MpiSystemType.SANTEMPI;
	
	private static final String SANTE_CLIENT_ID = "client_credentials";
	
	private static final String SANTE_CLIENT_SECRET = "bG6TuS3X-H1MsT4ctW!CxXjK9J4l1QpK8B0Q";
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
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
	
	@SuppressWarnings("static-access")
	@Test
	public void shouldInitIfNecessary() throws Exception {
		MpiContext context = mpiContext.initIfNecessary();
		
		assertFalse(context == null);
		assertEquals(context.getAuthenticationType(), AUTHENTICATION_TYPE);
		assertEquals(context.getClientId(), SANTE_CLIENT_ID);
		assertEquals(context.getClientSecret(), SANTE_CLIENT_SECRET);
		assertEquals(context.getMpiSystem(), MPI_SYSTEM);
		assertEquals(context.getServerBaseUrl(), MPI_BASE_URL);
		assertEquals(context.getContentType(), MPI_APP_CONTENT_TYPE);
		
	}
}
