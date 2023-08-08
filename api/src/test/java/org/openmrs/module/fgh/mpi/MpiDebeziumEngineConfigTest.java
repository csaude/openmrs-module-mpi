package org.openmrs.module.fgh.mpi;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.openmrs.api.context.Context.getRegisteredComponents;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.mysql.MySqlSnapshotMode;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class })
public class MpiDebeziumEngineConfigTest {
	
	@Mock
	private AdministrationService mockAdminService;
	
	@Mock
	private PatientAndPersonEventHandler mockHandler;
	
	@Mock
	private MpiHttpClient mockClient;
	
	private MpiDebeziumEngineConfig config;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		when(Context.getAdministrationService()).thenReturn(mockAdminService);
		when(getRegisteredComponents(PatientAndPersonEventHandler.class)).thenReturn(singletonList(mockHandler));
		when(getRegisteredComponents(MpiHttpClient.class)).thenReturn(singletonList(mockClient));
		config = new MpiDebeziumEngineConfig();
	}
	
	@Test
	public void init_shouldDefaultToTheNumberOfAvailableProcessorsAsTheThreadCountForInitialLoading() {
		Assert.assertNull(Whitebox.getInternalState(config, "eventProcessor"));
		config = Mockito.spy(config);
		Mockito.doAnswer(i -> MySqlSnapshotMode.INITIAL).when(config).getSnapshotMode();
		
		config.init();
		
		SnapshotEventProcessor processor = Whitebox.getInternalState(config, "eventProcessor");
		int threadCount = Whitebox.getInternalState(processor, "threadCount");
		Assert.assertEquals(Runtime.getRuntime().availableProcessors(), threadCount);
	}
	
	@Test
	public void init_shouldUseTheConfiguredBatchSizeAsTheThreadCount() {
		Assert.assertNull(Whitebox.getInternalState(config, "eventProcessor"));
		config = Mockito.spy(config);
		Mockito.doAnswer(i -> MySqlSnapshotMode.INITIAL).when(config).getSnapshotMode();
		final Integer BATCH_SIZE = 6;
		when(mockAdminService.getGlobalProperty(MpiConstants.GP_INITIAL_BATCH_SIZE)).thenReturn(BATCH_SIZE.toString());
		
		config.init();
		
		SnapshotEventProcessor processor = Whitebox.getInternalState(config, "eventProcessor");
		int threadCount = Whitebox.getInternalState(processor, "threadCount");
		Assert.assertEquals(BATCH_SIZE.intValue(), threadCount);
	}
	
}
