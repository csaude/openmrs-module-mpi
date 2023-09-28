package org.openmrs.module.fgh.mpi.api;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class MpiServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final Integer ENC_TYPE_ID = 1;
	
	@Autowired
	private MpiService service;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void getLocationForOldestEncounter_shouldGetTheLocationForTheOldestEncounterMatchingThePatientAndType() {
		executeDataSet("encounters.xml");
		Patient patient = Context.getPatientService().getPatient(6);
		EncounterType t = Context.getEncounterService().getEncounterType(ENC_TYPE_ID);
		assertEquals(2, service.getLocationForOldestEncounter(patient, t).getId().intValue());
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void getLocationForOldestEncounter_shouldReturnNullIfNoLocationCannotBeResolved() {
		executeDataSet("encounters.xml");
		Patient patient = Context.getPatientService().getPatient(8);
		EncounterType t = Context.getEncounterService().getEncounterType(ENC_TYPE_ID);
		Assert.assertNull(service.getLocationForOldestEncounter(patient, t));
	}
	
}
