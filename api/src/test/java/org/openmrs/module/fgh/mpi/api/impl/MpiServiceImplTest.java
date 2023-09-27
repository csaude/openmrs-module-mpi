package org.openmrs.module.fgh.mpi.api.impl;

import static org.junit.Assert.assertEquals;
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
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.fgh.mpi.MpiUtils;
import org.openmrs.module.fgh.mpi.api.db.MpiDAO;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MpiUtils.class)
public class MpiServiceImplTest {
	
	private MpiServiceImpl service = new MpiServiceImpl();
	
	@Mock
	private MpiDAO mockDao;
	
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
		setInternalState(service, MpiDAO.class, mockDao);
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
	public void getMostRecentLocation_shouldGetTheLocationOfTheMostRecentFichaEncounter() {
		setInternalState(MpiServiceImpl.class, "fichaEncType", (Object) null);
		when(mockDao.getMostRecentLocation(mockPatient, fichaEncType)).thenReturn(mockFichaFacility);
		assertEquals(mockFichaFacility, service.getMostRecentLocation(mockPatient));
	}
	
	@Test
	public void getMostRecentLocation_shouldGetTheLocationOfTheMostRecentAdultEncounter() {
		setInternalState(MpiServiceImpl.class, "adultProcessEncType", (Object) null);
		when(mockDao.getMostRecentLocation(mockPatient, fichaEncType)).thenReturn(mockAdultFacility);
		assertEquals(mockAdultFacility, service.getMostRecentLocation(mockPatient));
	}
	
	@Test
	public void getMostRecentLocation_shouldGetTheLocationOfTheMostRecentChildEncounter() {
		setInternalState(MpiServiceImpl.class, "childProcessEncType", (Object) null);
		when(mockDao.getMostRecentLocation(mockPatient, fichaEncType)).thenReturn(mockChildFacility);
		assertEquals(mockChildFacility, service.getMostRecentLocation(mockPatient));
	}
	
	@Test
	public void getMostRecentLocation_shouldReturnNullIfNoLocationIsFound() {
		Assert.assertNull(service.getMostRecentLocation(mockPatient));
	}
	
	@Test
	public void getMostRecentLocation_shouldNotLoadTheEncounterTypesIfAlreadySet() {
		setInternalState(MpiServiceImpl.class, "fichaEncType", fichaEncType);
		setInternalState(MpiServiceImpl.class, "adultProcessEncType", adultEncType);
		setInternalState(MpiServiceImpl.class, "childProcessEncType", childEncType);
		when(mockDao.getMostRecentLocation(mockPatient, fichaEncType)).thenReturn(mockFichaFacility);
		
		assertEquals(mockFichaFacility, service.getMostRecentLocation(mockPatient));
		
		PowerMockito.verifyZeroInteractions(MpiUtils.class);
	}
	
}
