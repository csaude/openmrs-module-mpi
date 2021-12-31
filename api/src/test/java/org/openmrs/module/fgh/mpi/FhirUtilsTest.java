package org.openmrs.module.fgh.mpi;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.FhirUtils.ATTR_QUERY;
import static org.openmrs.module.fgh.mpi.FhirUtils.ATTR_TYPE_ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiConstants.DATETIME_FORMATTER;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_ATTRIB_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.IDENTIFIER;
import static org.openmrs.module.fgh.mpi.MpiConstants.NAME;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiUtils.executeQuery;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Location;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
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
	
	@Mock
	private LocationService mockLocationService;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		when(Context.getAdministrationService()).thenReturn(mockAdminService);
		when(Context.getPersonService()).thenReturn(mockPersonService);
		when(Context.getLocationService()).thenReturn(mockLocationService);
	}
	
	@Test
	public void buildPatient_shouldBuildAPatientResource() throws Exception {
		final String patientId = "1";
		final String birthDate = "1986-10-07";
		final boolean dead = false;
		final String patientUuid = "person-uuid";
		final boolean personVoided = false;
		final boolean patientVoided = false;
		List<Object> personDetails = Arrays.asList("M", birthDate, dead, null, patientUuid, personVoided);
		final String identifier1 = "12345";
		final String idTypeUuid1 = "id-type-uuid-1";
		final String idUuid1 = "id-uuid-1";
		List<Object> id1 = Arrays.asList(identifier1, idTypeUuid1, idUuid1);
		final String identifier2 = "qwerty";
		final String idTypeUuid2 = "id-type-uuid-2";
		final String idUuid2 = "id-uuid-2";
		List<Object> id2 = Arrays.asList(identifier2, idTypeUuid2, idUuid2);
		List<List<Object>> ids = Arrays.asList(id1, id2);
		when(executeQuery(FhirUtils.ID_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(ids);
		final String prefix1 = "Mr";
		final String givenName1 = "Horatio";
		final String middleName1 = "D";
		final String familyName1 = "HornBlower";
		final String nameUuid1 = "name-uuid-1";
		List<Object> name1 = Arrays.asList(prefix1, givenName1, middleName1, familyName1, nameUuid1);
		final String prefix2 = "Miss";
		final String givenName2 = "John";
		final String middleName2 = "S";
		final String familyName2 = "Doe";
		final String nameUuid2 = "name-uuid-2";
		List<Object> name2 = Arrays.asList(prefix2, givenName2, middleName2, familyName2, nameUuid2);
		List<List<Object>> names = Arrays.asList(name1, name2);
		when(executeQuery(FhirUtils.NAME_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(names);
		final String line1Address2 = "123";
		final String line1Address6 = "Ocean";
		final String line1Address5 = "Dr";
		final String line1Address3 = "Apt";
		final String line1Address1 = "A";
		final String countyDistrict1 = "Travis";
		final String stateProvince1 = "Texas";
		final String country1 = "US";
		final String startDate1 = "2020-01-01 00:00:00";
		final String endDate1 = "2020-12-31 00:00:00";
		final String addressUuid1 = "address-uuid-1";
		List<Object> personAddress1 = Arrays.asList(line1Address1, line1Address2, line1Address3, line1Address5,
		    line1Address6, countyDistrict1, stateProvince1, country1, startDate1, endDate1, addressUuid1);
		final String line2Address2 = "987";
		final String line2Address6 = "Rubaga";
		final String line2Address5 = "Rd";
		final String line2Address3 = "Unit";
		final String line2Address1 = "1";
		final String countyDistrict2 = "Kampala";
		final String stateProvince2 = "Central Region";
		final String country2 = "Uganda";
		final String startDate2 = "2009-01-01 00:00:00";
		final String endDate2 = "2009-12-31 00:00:00";
		final String addressUuid2 = "address-uuid-2";
		List<Object> personAddress2 = Arrays.asList(line2Address1, line2Address2, line2Address3, line2Address5,
		    line2Address6, countyDistrict2, stateProvince2, country2, startDate2, endDate2, addressUuid2);
		List<List<Object>> addresses = new ArrayList();
		addresses.add(personAddress1);
		addresses.add(personAddress2);
		when(executeQuery(FhirUtils.ADDRESS_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(addresses);
		
		final String mobile = "123-456-7890";
		final String attributeUuid1 = "attr-uuid-1";
		final Integer mobileAttrTypeId = 1;
		final String mobileAttrTypeUuid = "attr-type-uuid-1";
		List<Object> mobileAttr = Arrays.asList(mobile, attributeUuid1);
		when(mockAdminService.getGlobalProperty(MpiConstants.GP_PHONE_MOBILE)).thenReturn(mobileAttrTypeUuid);
		PersonAttributeType mobileAttrType = new PersonAttributeType(mobileAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(mobileAttrTypeUuid)).thenReturn(mobileAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, mobileAttrTypeId.toString())))
		            .thenReturn(singletonList(mobileAttr));
		final String home = "098-765-4321";
		final String attributeUuid2 = "attr-uuid-2";
		final Integer homeAttrTypeId = 2;
		final String homeAttrTypeUuid = "attr-type-uuid-2";
		List<Object> homePhoneAttr = Arrays.asList(home, attributeUuid2);
		when(mockAdminService.getGlobalProperty(MpiConstants.GP_PHONE_HOME)).thenReturn(homeAttrTypeUuid);
		PersonAttributeType homeAttrType = new PersonAttributeType(homeAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(homeAttrTypeUuid)).thenReturn(homeAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, homeAttrTypeId.toString())))
		            .thenReturn(singletonList(homePhoneAttr));
		
		final Integer locationId = 1;
		final String locationUuid = "location-uuid";
		final String locationName = "Location";
		final Integer healthCtrAttrTypeId = 6;
		List<Object> healthCenter = Arrays.asList(locationId.toString(), "attrib-uuid");
		PersonAttributeType healthCenterAttrType = new PersonAttributeType(healthCtrAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(HEALTH_CENTER_ATTRIB_TYPE_UUID))
		        .thenReturn(healthCenterAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, healthCtrAttrTypeId.toString())))
		            .thenReturn(singletonList(healthCenter));
		Location location = new Location(locationId);
		location.setName(locationName);
		location.setUuid(locationUuid);
		when(mockLocationService.getLocation(locationId)).thenReturn(location);
		
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
		assertEquals(idUuid1, resourceIds.get(1).get(MpiConstants.FIELD_ID));
		assertEquals(MpiConstants.SYSTEM_PREFIX + idTypeUuid2, resourceIds.get(2).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(identifier2, resourceIds.get(2).get(MpiConstants.FIELD_VALUE));
		assertEquals(idUuid2, resourceIds.get(2).get(MpiConstants.FIELD_ID));
		
		List<Map> resourceNames = (List) resource.get(MpiConstants.FIELD_NAME);
		assertEquals(2, resourceNames.size());
		assertEquals(MpiConstants.USE_OFFICIAL, resourceNames.get(0).get(MpiConstants.FIELD_USE));
		assertEquals(prefix1, resourceNames.get(0).get(MpiConstants.FIELD_PREFIX));
		assertEquals(nameUuid1, resourceNames.get(0).get(MpiConstants.FIELD_ID));
		assertEquals(familyName1, resourceNames.get(0).get(MpiConstants.FIELD_FAMILY));
		List<Object> givenNames1 = (List) resourceNames.get(0).get(MpiConstants.FIELD_GIVEN);
		assertEquals(givenName1, givenNames1.get(0));
		assertEquals(middleName1, givenNames1.get(1));
		assertNull(resourceNames.get(1).get(MpiConstants.FIELD_USE));
		assertEquals(prefix2, resourceNames.get(1).get(MpiConstants.FIELD_PREFIX));
		assertEquals(nameUuid2, resourceNames.get(1).get(MpiConstants.FIELD_ID));
		assertEquals(familyName2, resourceNames.get(1).get(MpiConstants.FIELD_FAMILY));
		List<Object> givenNames2 = (List) resourceNames.get(1).get(MpiConstants.FIELD_GIVEN);
		assertEquals(givenName2, givenNames2.get(0));
		assertEquals(middleName2, givenNames2.get(1));
		
		List<Map> resourceAddresses = (List) resource.get(MpiConstants.FIELD_ADDRESS);
		assertEquals(2, resourceAddresses.size());
		List<Object> line1 = (List) resourceAddresses.get(0).get(MpiConstants.FIELD_LINE);
		assertEquals(line1Address2, line1.get(0));
		assertEquals(line1Address6, line1.get(1));
		assertEquals(line1Address5, line1.get(2));
		assertEquals(line1Address3, line1.get(3));
		assertEquals(line1Address1, line1.get(4));
		Map period1 = (Map) resourceAddresses.get(0).get(MpiConstants.FIELD_PERIOD);
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(startDate1)), period1.get(MpiConstants.FIELD_START));
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(endDate1)), period1.get(MpiConstants.FIELD_END));
		assertEquals(countyDistrict1, resourceAddresses.get(0).get(MpiConstants.FIELD_DISTRICT));
		assertEquals(stateProvince1, resourceAddresses.get(0).get(MpiConstants.FIELD_STATE));
		assertEquals(country1, resourceAddresses.get(0).get(MpiConstants.FIELD_COUNTRY));
		assertEquals(addressUuid1, resourceAddresses.get(0).get(MpiConstants.FIELD_ID));
		List<Object> line2 = (List) resourceAddresses.get(1).get(MpiConstants.FIELD_LINE);
		assertEquals(line2Address2, line2.get(0));
		assertEquals(line2Address6, line2.get(1));
		assertEquals(line2Address5, line2.get(2));
		assertEquals(line2Address3, line2.get(3));
		assertEquals(line2Address1, line2.get(4));
		Map period2 = (Map) resourceAddresses.get(1).get(MpiConstants.FIELD_PERIOD);
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(startDate2)), period2.get(MpiConstants.FIELD_START));
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(endDate2)), period2.get(MpiConstants.FIELD_END));
		assertEquals(countyDistrict2, resourceAddresses.get(1).get(MpiConstants.FIELD_DISTRICT));
		assertEquals(stateProvince2, resourceAddresses.get(1).get(MpiConstants.FIELD_STATE));
		assertEquals(country2, resourceAddresses.get(1).get(MpiConstants.FIELD_COUNTRY));
		assertEquals(addressUuid2, resourceAddresses.get(1).get(MpiConstants.FIELD_ID));
		
		List<Map> resourceTelecoms = (List) resource.get(MpiConstants.FIELD_TELECOM);
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(0).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.MOBILE, resourceTelecoms.get(0).get(MpiConstants.FIELD_USE));
		assertEquals(mobile, resourceTelecoms.get(0).get(MpiConstants.FIELD_VALUE));
		assertEquals(attributeUuid1, resourceTelecoms.get(0).get(MpiConstants.FIELD_ID));
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(1).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.HOME, resourceTelecoms.get(1).get(MpiConstants.FIELD_USE));
		assertEquals(home, resourceTelecoms.get(1).get(MpiConstants.FIELD_VALUE));
		assertEquals(attributeUuid2, resourceTelecoms.get(1).get(MpiConstants.FIELD_ID));
		
		List<Map> extension = (List) resource.get(MpiConstants.FIELD_EXTENSION);
		assertEquals(1, extension.size());
		assertEquals(HEALTH_CENTER_URL, extension.get(0).get(MpiConstants.FIELD_URL));
		extension = (List) extension.get(0).get(MpiConstants.FIELD_EXTENSION);
		assertEquals(IDENTIFIER, extension.get(0).get(MpiConstants.FIELD_URL));
		assertEquals(locationUuid, extension.get(0).get(MpiConstants.FIELD_VALUE_STR));
		assertEquals(NAME, extension.get(1).get(MpiConstants.FIELD_URL));
		assertEquals(locationName, extension.get(1).get(MpiConstants.FIELD_VALUE_STR));
	}
	
}
