package org.openmrs.module.fgh.mpi;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.EncounterType;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
@SuppressStaticInitializationFor({ "org.openmrs.api.context.Context", "org.openmrs.module.fgh.mpi.MpiUtils" })
public class MpiUtilsTest {
	
	@Mock
	private AdministrationService mockAdminService;
	
	@Mock
	private EncounterService encounterService;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		when(Context.getAdministrationService()).thenReturn(mockAdminService);
		when(Context.getEncounterService()).thenReturn(encounterService);
	}
	
	@Test
	public void getGlobalPropertyValue_shouldReturnTheValueOfTheGlobalProperty() {
		final String gpName = "test";
		final String expectedValue = "value";
		when(mockAdminService.getGlobalProperty(gpName)).thenReturn(expectedValue);
		
		assertEquals(expectedValue, MpiUtils.getGlobalPropertyValue(gpName));
	}
	
	@Test
	public void getGlobalPropertyValue_shouldFailIfTheValueIsNull() {
		final String gpName = "test";
		when(mockAdminService.getGlobalProperty(gpName)).thenReturn(null);
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No value set for the global property named: " + gpName));
		MpiUtils.getGlobalPropertyValue(gpName);
	}
	
	@Test
	public void getGlobalPropertyValue_shouldFailIfTheValueIsBlank() {
		final String gpName = "test";
		when(mockAdminService.getGlobalProperty(gpName)).thenReturn("");
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No value set for the global property named: " + gpName));
		MpiUtils.getGlobalPropertyValue(gpName);
	}
	
	@Test
	public void getGlobalPropertyValue_shouldFailIfTheValueIsAWhitespaceCharacter() {
		final String gpName = "test";
		when(mockAdminService.getGlobalProperty(gpName)).thenReturn(" ");
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No value set for the global property named: " + gpName));
		MpiUtils.getGlobalPropertyValue(gpName);
	}
	
	@Test
	public void getEncounterTypeByGlobalProperty_shouldReturnTheMatchingEncounterType() {
		final String gpName = "test";
		final String typeUuid = "uuid";
		EncounterType expectedType = Mockito.mock(EncounterType.class);
		when(mockAdminService.getGlobalProperty(gpName)).thenReturn(typeUuid);
		when(encounterService.getEncounterTypeByUuid(typeUuid)).thenReturn(expectedType);
		
		assertEquals(expectedType, MpiUtils.getEncounterTypeByGlobalProperty(gpName));
	}
	
	@Test
	public void getEncounterTypeByGlobalProperty_shouldFailIfNoMatchingEncounterTypeIsFound() {
		final String gpName = "test";
		final String typeUuid = "uuid";
		when(mockAdminService.getGlobalProperty(gpName)).thenReturn(typeUuid);
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No encounter found matching uuid: " + typeUuid));
		MpiUtils.getEncounterTypeByGlobalProperty(gpName);
	}
	
}
