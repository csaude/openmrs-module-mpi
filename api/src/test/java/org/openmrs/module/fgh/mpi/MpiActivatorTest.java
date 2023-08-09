package org.openmrs.module.fgh.mpi;

import static org.apache.commons.lang3.reflect.ConstructorUtils.getAccessibleConstructor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.MpiActivator.DIR_LOGS;
import static org.openmrs.module.fgh.mpi.MpiActivator.DIR_MPI;
import static org.openmrs.module.fgh.mpi.MpiActivator.LOG_FILE;
import static org.openmrs.module.fgh.mpi.MpiActivator.LOG_FILE_DATE_FORMAT;
import static org.openmrs.module.fgh.mpi.MpiActivator.MPI_APPENDER_NAME;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.util.OpenmrsUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OpenmrsUtil.class, MpiUtils.class, MpiActivator.class })
public class MpiActivatorTest {
	
	private MpiActivator activator;
	
	private static final String TEST_APP_DIR = "some/dir";
	
	private static final String TEST_LOG_FILE = "some/file";
	
	@Mock
	private Path mockPath;
	
	@Mock
	private File mockFile;
	
	@Mock
	private Logger mockMpiLogger;
	
	@Mock
	private PatternLayout mockLayout;
	
	private DailyRollingFileAppender testAppender;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(OpenmrsUtil.class);
		PowerMockito.mockStatic(MpiUtils.class);
		when(OpenmrsUtil.getApplicationDataDirectory()).thenReturn(TEST_APP_DIR);
		when(mockFile.getAbsolutePath()).thenReturn(TEST_LOG_FILE);
		when(MpiUtils.createPath(TEST_APP_DIR, DIR_MPI, DIR_LOGS, LOG_FILE)).thenReturn(mockPath);
		when(mockPath.toFile()).thenReturn(mockFile);
		testAppender = new DailyRollingFileAppender();
		activator = new MpiActivator();
	}
	
	@Test
	public void started_shouldAddTheMpiLogFileToTheLog4jConfig() throws Exception {
		Assert.assertNotEquals(MPI_APPENDER_NAME, testAppender.getName());
		Constructor<PatternLayout> layoutConstructor = getAccessibleConstructor(PatternLayout.class, String.class);
		whenNew(layoutConstructor).withArguments(MpiActivator.LAYOUT).thenReturn(mockLayout);
		Constructor<DailyRollingFileAppender> appenderConstructor = getAccessibleConstructor(DailyRollingFileAppender.class,
		    Layout.class, String.class, String.class);
		whenNew(appenderConstructor).withArguments(mockLayout, TEST_LOG_FILE, LOG_FILE_DATE_FORMAT).thenReturn(testAppender);
		testAppender.setName(MPI_APPENDER_NAME);
		activator = Mockito.spy(activator);
		when(activator.getMpiLogger()).thenReturn(mockMpiLogger);
		
		activator.started();
		
		Assert.assertEquals(MPI_APPENDER_NAME, testAppender.getName());
		verify(mockMpiLogger).setAdditivity(false);
		verify(mockMpiLogger).addAppender(testAppender);
	}
	
	@Test
	public void stopped_shouldRemoveTheMpiLogFileFromTheLog4jConfig() {
		activator = Mockito.spy(activator);
		when(activator.getMpiLogger()).thenReturn(mockMpiLogger);
		when(mockMpiLogger.getAppender(MPI_APPENDER_NAME)).thenReturn(testAppender);
		
		activator.stopped();
		
		verify(mockMpiLogger).removeAppender(MPI_APPENDER_NAME);
	}
	
}
