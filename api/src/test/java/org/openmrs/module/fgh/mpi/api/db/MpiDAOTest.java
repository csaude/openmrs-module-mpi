package org.openmrs.module.fgh.mpi.api.db;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class MpiDAOTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private MpiDAO dao;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void getMostRecentLocation_shouldGetTheLocationOfTheMostRecentEncounterMatchingThePatientAndType() {
		executeDataSet("encounters.xml");
		Patient patient = Context.getPatientService().getPatient(6);
		EncounterType t = Context.getEncounterService().getEncounterTypeByUuid("61ae96f4-6afe-4351-b6f8-cd4fc383cce1");
		assertEquals("9356400c-a5a2-4532-8f2b-2361b3446eb8", dao.getMostRecentLocation(patient, t).getUuid());
	}
	
}
