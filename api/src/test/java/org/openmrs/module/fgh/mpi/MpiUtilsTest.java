package org.openmrs.module.fgh.mpi;

import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class MpiUtilsTest {
	
	@Mock
	private AdministrationService mockAdminService;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getAdministrationService()).thenReturn(mockAdminService);
	}
	
	@Test
	public void getGlobalPropertyValue_shouldReturnTheValueOfTheGlobalProperty() {
		final String gpName = "test";
		final String expectedValue = "value";
		Mockito.when(mockAdminService.getGlobalProperty(gpName)).thenReturn(expectedValue);
		
		Assert.assertEquals(expectedValue, MpiUtils.getGlobalPropertyValue(gpName));
	}
	
	@Test
	public void getGlobalPropertyValue_shouldFailIfTheValueIsNull() {
		final String gpName = "test";
		Mockito.when(mockAdminService.getGlobalProperty(gpName)).thenReturn(null);
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No value set for the global property named: " + gpName));
		MpiUtils.getGlobalPropertyValue(gpName);
	}
	
	@Test
	public void getGlobalPropertyValue_shouldFailIfTheValueIsBlank() {
		final String gpName = "test";
		Mockito.when(mockAdminService.getGlobalProperty(gpName)).thenReturn("");
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No value set for the global property named: " + gpName));
		MpiUtils.getGlobalPropertyValue(gpName);
	}
	
	@Test
	public void getGlobalPropertyValue_shouldFailIfTheValueIsAWhitespaceCharacter() {
		final String gpName = "test";
		Mockito.when(mockAdminService.getGlobalProperty(gpName)).thenReturn(" ");
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No value set for the global property named: " + gpName));
		MpiUtils.getGlobalPropertyValue(gpName);
	}
	
}
