package org.openmrs.module.fgh.mpi.api.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_ADULT_PROCESS_ENC_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_CHILD_PROCESS_ENC_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_FICHA_RESUMO_ENC_TYPE_UUID;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.fgh.mpi.MpiUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MpiUtils.class)
public class MpiServiceImplTest {
	
	private MpiServiceImpl service = new MpiServiceImpl();
	
	@Mock
	private EncounterType fichaEncType;
	
	@Mock
	private EncounterType adultEncType;
	
	@Mock
	private EncounterType childEncType;
	
	@Mock
	private Patient mockPatient;
	
	@Mock
	private Location mockFichaFacility;
	
	@Mock
	private Location mockAdultFacility;
	
	@Mock
	private Location mockChildFacility;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(MpiUtils.class);
		when(MpiUtils.getEncounterTypeByGlobalProperty(GP_FICHA_RESUMO_ENC_TYPE_UUID)).thenReturn(fichaEncType);
		when(MpiUtils.getEncounterTypeByGlobalProperty(GP_ADULT_PROCESS_ENC_TYPE_UUID)).thenReturn(adultEncType);
		when(MpiUtils.getEncounterTypeByGlobalProperty(GP_CHILD_PROCESS_ENC_TYPE_UUID)).thenReturn(childEncType);
	}
	
	@After
	public void tearDown() {
		setInternalState(MpiServiceImpl.class, "fichaEncType", (Object) null);
		setInternalState(MpiServiceImpl.class, "adultProcessEncType", (Object) null);
		setInternalState(MpiServiceImpl.class, "childProcessEncType", (Object) null);
	}
	
	@Test
	public void getHealthFacility_shouldGetTheLocationOfTheOldestFichaEncounter() {
		setInternalState(MpiServiceImpl.class, "fichaEncType", (Object) null);
		service = Mockito.spy(service);
		doReturn(mockFichaFacility).when(service).getLocationForOldestEncounter(mockPatient, fichaEncType);
		assertEquals(mockFichaFacility, service.getHealthFacility(mockPatient));
		Mockito.verify(service).getLocationForOldestEncounter(mockPatient, fichaEncType);
		Mockito.verify(service, never()).getLocationForOldestEncounter(mockPatient, adultEncType);
		Mockito.verify(service, never()).getLocationForOldestEncounter(mockPatient, childEncType);
	}
	
	@Test
	public void getHealthFacility_shouldGetTheLocationOfTheOldestAdultEncounter() {
		setInternalState(MpiServiceImpl.class, "adultProcessEncType", (Object) null);
		service = Mockito.spy(service);
		doReturn(mockAdultFacility).when(service).getLocationForOldestEncounter(mockPatient, adultEncType);
		assertEquals(mockAdultFacility, service.getHealthFacility(mockPatient));
		Mockito.verify(service).getLocationForOldestEncounter(mockPatient, fichaEncType);
		Mockito.verify(service).getLocationForOldestEncounter(mockPatient, adultEncType);
		Mockito.verify(service, never()).getLocationForOldestEncounter(mockPatient, childEncType);
	}
	
	@Test
	public void getHealthFacility_shouldGetTheLocationOfTheOldestChildEncounter() {
		setInternalState(MpiServiceImpl.class, "childProcessEncType", (Object) null);
		service = Mockito.spy(service);
		doReturn(mockChildFacility).when(service).getLocationForOldestEncounter(mockPatient, childEncType);
		assertEquals(mockChildFacility, service.getHealthFacility(mockPatient));
		Mockito.verify(service).getLocationForOldestEncounter(mockPatient, fichaEncType);
		Mockito.verify(service).getLocationForOldestEncounter(mockPatient, adultEncType);
		Mockito.verify(service).getLocationForOldestEncounter(mockPatient, childEncType);
	}
	
	@Test
	public void getHealthFacility_shouldReturnNullIfNoLocationIsFound() {
		Assert.assertNull(service.getHealthFacility(mockPatient));
	}
	
	@Test
	public void getHealthFacility_shouldNotLoadTheEncounterTypesIfAlreadySet() {
		setInternalState(MpiServiceImpl.class, "fichaEncType", fichaEncType);
		setInternalState(MpiServiceImpl.class, "adultProcessEncType", adultEncType);
		setInternalState(MpiServiceImpl.class, "childProcessEncType", childEncType);
		service = Mockito.spy(service);
		doReturn(mockFichaFacility).when(service).getLocationForOldestEncounter(mockPatient, fichaEncType);
		
		assertEquals(mockFichaFacility, service.getHealthFacility(mockPatient));
		
		PowerMockito.verifyZeroInteractions(MpiUtils.class);
	}
	
}
