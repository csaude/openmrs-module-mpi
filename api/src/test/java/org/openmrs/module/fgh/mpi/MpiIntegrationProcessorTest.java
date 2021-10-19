package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.debezium.DatabaseOperation.CREATE;
import static org.openmrs.module.debezium.DatabaseOperation.READ;
import static org.openmrs.module.debezium.DatabaseOperation.UPDATE;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class MpiIntegrationProcessorTest {
	
	@Mock
	private MpiHttpClient mockMpiHttpClient;
	
	@Mock
	private Logger mockLogger;
	
	@Mock
	private AdministrationService mockAdminService;
	
	private MpiIntegrationProcessor processor = new MpiIntegrationProcessor();
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		when(Context.getAdministrationService()).thenReturn(mockAdminService);
		Whitebox.setInternalState(MpiIntegrationProcessor.class, Logger.class, mockLogger);
	}
	
	@Test
	public void process_shouldIgnoreAPersonInsertEvent() throws Exception {
		assertNull(processor.process(null, new DatabaseEvent(null, "person", CREATE, null, null, null)));
		verify(mockLogger).info("Ignoring person insert event");
	}
	
	@Test
	public void process_shouldIgnoreAPersonUpdateEventIfThePersonRowNoLongerExists() throws Exception {
		final Integer personId = 1;
		assertNull(processor.process(personId, new DatabaseEvent(null, "person", UPDATE, null, null, null)));
		verify(mockLogger).info("Ignoring event because no person was found with id: " + personId);
	}
	
	@Test
	public void process_shouldIgnoreAPersonReadEventIfThePersonRowNoLongerExists() throws Exception {
		final Integer personId = 1;
		assertNull(processor.process(personId, new DatabaseEvent(null, "person", READ, null, null, null)));
		verify(mockLogger).info("Ignoring event because no person was found with id: " + personId);
	}
	
	@Test
	public void process_shouldIgnoreAPersonDeleteEventIfThePatientDoesNotExistInTheMpi() throws Exception {
		
	}
	
	@Test
	public void process_shouldIgnoreAPatientDeleteEventIfThePatientDoesNotExistInTheMpi() throws Exception {
		
	}
	
	@Test
	public void process_shouldIgnoreAPersonDeleteEventIfTheTheMpiPatientRecordIsInactive() throws Exception {
		
	}
	
	@Test
	public void process_shouldIgnoreAPatientDeleteEventIfTheMpiPatientRecordIsInactive() throws Exception {
		
	}
	
	@Test
	public void process_shouldProcessAPatientThatDoesNotExistInTheMpi() throws Exception {
		processor.process(1, new DatabaseEvent(null, null, null, null, null, null));
	}
	
	@Test
	public void process_shouldProcessAPatientThatAlreadyExistsInTheMpi() throws Exception {
	}
	
	@Test
	public void process_shouldProcessAPersonDeleteEvent() throws Exception {
	}
	
	@Test
	public void process_shouldProcessAPatientDeleteEvent() throws Exception {
	}
	
}
