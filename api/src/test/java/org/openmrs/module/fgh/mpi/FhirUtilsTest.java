package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class })
public class FhirUtilsTest {
	
	@Mock
	private AdministrationService mockAdminService;
	
	@Mock
	private PersonService mockPersonService;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		when(Context.getAdministrationService()).thenReturn(mockAdminService);
		when(Context.getPersonService()).thenReturn(mockPersonService);
	}
	
	@Test
	public void buildPatient_shouldBuildAPatientResource() throws Exception {
		final String patientId = "1";
		List<Object> personDetails = new ArrayList();
		final String birthDate = "1986-10-07";
		final boolean dead = false;
		final String patientUuid = "person-uuid";
		final boolean personVoided = false;
		final boolean patientVoided = false;
		final String identifier1 = "12345";
		final String idTypeUuid1 = "id-type-uuid-1";
		final String idUuid = "id-uuid-1";
		personDetails.add("M");
		personDetails.add(birthDate);
		personDetails.add(dead);
		personDetails.add(null);
		personDetails.add(patientUuid);
		personDetails.add(personVoided);
		List<Object> id1 = new ArrayList();
		id1.add(identifier1);
		id1.add(idTypeUuid1);
		id1.add(idUuid);
		List<Object> id2 = new ArrayList();
		id2.add("qwerty");
		id2.add("id-type-uuid-2");
		id2.add("id-uuid-2");
		List<List<Object>> ids = new ArrayList();
		ids.add(id1);
		ids.add(id2);
		when(MpiUtils.executeQuery(FhirUtils.ID_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(ids);
		final String prefix1 = "Mr";
		final String givenName1 = "Horatio";
		final String middleName1 = "D";
		final String familyName1 = "HornBlower";
		final String nameUuid1 = "name-uuid-1";
		final String prefix2 = "Miss";
		final String givenName2 = "John";
		final String middleName2 = "S";
		final String familyName2 = "Doe";
		final String nameUuid2 = "name-uuid-2";
		List<Object> name1 = new ArrayList();
		name1.add(prefix1);
		name1.add(givenName1);
		name1.add(middleName1);
		name1.add(familyName1);
		name1.add(nameUuid1);
		List<Object> name2 = new ArrayList();
		name2.add(prefix2);
		name2.add(givenName2);
		name2.add(middleName2);
		name2.add(familyName2);
		name2.add(nameUuid2);
		List<List<Object>> names = new ArrayList();
		names.add(name1);
		names.add(name2);
		when(MpiUtils.executeQuery(FhirUtils.NAME_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(names);
		
		when(mockPersonService.getPersonAttributeTypeByUuid(anyString())).thenReturn(new PersonAttributeType(1));
		
		Map<String, Object> resource = FhirUtils.buildPatient("1", patientVoided, personDetails, null);
		
		System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resource));
		assertEquals(MpiConstants.PATIENT, resource.get(MpiConstants.FIELD_RESOURCE_TYPE));
		assertEquals(MpiConstants.GENDER_MALE, resource.get(MpiConstants.FIELD_GENDER));
		assertEquals(!patientVoided, resource.get(MpiConstants.FIELD_ACTIVE));
		assertEquals(birthDate, resource.get(MpiConstants.FIELD_BIRTHDATE));
		assertEquals(dead, resource.get(MpiConstants.FIELD_DECEASED));
		List<Map> resourceIds = (List) resource.get(MpiConstants.FIELD_IDENTIFIER);
		assertEquals(3, resourceIds.size());
		assertEquals(MpiConstants.SOURCE_ID_URI, resourceIds.get(0).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(patientUuid, resourceIds.get(0).get(MpiConstants.FIELD_VALUE));
		assertEquals(MpiConstants.SYSTEM_PREFIX + idTypeUuid1, resourceIds.get(1).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(identifier1, resourceIds.get(1).get(MpiConstants.FIELD_VALUE));
		assertEquals(idUuid, resourceIds.get(1).get(MpiConstants.FIELD_ID));
		List<Map> resourceNames = (List) resource.get(MpiConstants.FIELD_NAME);
		assertEquals(2, resourceNames.size());
		assertEquals(MpiConstants.USE_OFFICIAL, resourceNames.get(0).get(MpiConstants.FIELD_USE));
		assertEquals(prefix1, resourceNames.get(0).get(MpiConstants.FIELD_PREFIX));
		assertEquals(nameUuid1, resourceNames.get(0).get(MpiConstants.FIELD_ID));
		assertEquals(familyName1, resourceNames.get(0).get(MpiConstants.FIELD_FAMILY));
		List<Object> givenNames = (List) resourceNames.get(0).get(MpiConstants.FIELD_GIVEN);
		assertEquals(givenName1, givenNames.get(0));
		assertEquals(middleName1, givenNames.get(1));
	}
	
}
