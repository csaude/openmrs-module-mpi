package org.openmrs.module.fgh.mpi.api.impl;

import static org.junit.Assert.assertNull;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_HEALTH_CENTER_ENC_TYPE_UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.powermock.reflect.Whitebox;

public class MpiServiceImplTest {
	
	private MpiServiceImpl service = new MpiServiceImpl();
	
	@Before
	public void setup() {
		Whitebox.setInternalState(MpiServiceImpl.class, EncounterType.class, (Object) null);
	}
	
	@Test
	public void supportsPropertyName_shouldReturnTrueForTheHealthCenterGlobalProperty() {
		Assert.assertTrue(service.supportsPropertyName(GP_HEALTH_CENTER_ENC_TYPE_UUID));
	}
	
	@Test
	public void supportsPropertyName_shouldReturnFalseForTheHealthCenterGlobalProperty() {
		Assert.assertFalse(service.supportsPropertyName("some.other.gp"));
	}
	
	@Test
	public void globalPropertyChanged_shouldClearHealthCenterEncounterType() {
		Whitebox.setInternalState(MpiServiceImpl.class, new EncounterType());
		service.globalPropertyChanged(new GlobalProperty());
		assertNull(Whitebox.getInternalState(MpiServiceImpl.class, EncounterType.class));
	}
	
	@Test
	public void globalPropertyDeleted_shouldClearHealthCenterEncounterType() {
		Whitebox.setInternalState(MpiServiceImpl.class, new EncounterType());
		service.globalPropertyDeleted(GP_HEALTH_CENTER_ENC_TYPE_UUID);
		assertNull(Whitebox.getInternalState(MpiServiceImpl.class, EncounterType.class));
	}
	
}
