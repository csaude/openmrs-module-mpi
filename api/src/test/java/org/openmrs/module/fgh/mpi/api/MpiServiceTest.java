package org.openmrs.module.fgh.mpi.api;

import static org.junit.Assert.assertEquals;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fgh.mpi.MpiConstants;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class MpiServiceTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private AdministrationService adminService;
	
	@Autowired
	private MpiService mpiService;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void getMostRecentLocation_shouldGetTheLocationOfTheMostRecentEncounterOfTheConfiguredHealthCenterType() {
		executeDataSet("encounters.xml");
		Patient patient = patientService.getPatient(6);
		assertEquals("9356400c-a5a2-4532-8f2b-2361b3446eb8", mpiService.getMostRecentLocation(patient).getUuid());
	}
	
	@Test
	public void getMostRecentLocation_shouldReturnNullIfNoLocationCanBeResolved() {
		executeDataSet("encounters.xml");
		Assert.assertNull(mpiService.getMostRecentLocation(patientService.getPatient(8)));
	}
	
	@Test
	public void getMostRecentLocation_shouldFailIfTheNoEncounterMatchesTheConfiguredUuid() {
		final String encTypeUuid = "some-enc-type";
		GlobalProperty gp = new GlobalProperty(MpiConstants.GP_HEALTH_CENTER_ENC_TYPE_UUID, encTypeUuid);
		adminService.saveGlobalProperty(gp);
		expectedException.expect(APIException.class);
		expectedException.expectMessage(Matchers.equalTo("No encounter found matching uuid: " + encTypeUuid));
		mpiService.getMostRecentLocation(patientService.getPatient(2));
	}
	
}
