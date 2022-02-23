package org.openmrs.module.fgh.mpi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.FhirUtils.ADDRESS_QUERY;
import static org.openmrs.module.fgh.mpi.FhirUtils.ATTR_QUERY;
import static org.openmrs.module.fgh.mpi.FhirUtils.ATTR_TYPE_ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.FhirUtils.CONTACT_PERSON_QUERY;
import static org.openmrs.module.fgh.mpi.FhirUtils.NAME_QUERY;
import static org.openmrs.module.fgh.mpi.FhirUtils.RELATIONSHIP_QUERY;
import static org.openmrs.module.fgh.mpi.MpiConstants.DATETIME_FORMATTER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ADDRESS;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CODE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CODING;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CONTACT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_DISPLAY;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_END;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_EXTENSION;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_GENDER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_GIVEN;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_NAME;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_PREFIX;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_RELATIONSHIP;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_START;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_TELECOM;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_TEXT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_USE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_VALUE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_FEMALE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_MALE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_OTHER;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_UNKNOWN;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_PHONE_HOME;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_PHONE_MOBILE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_RELATIONSHIP_TYPE_CONCEPT_MAP_A;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_RELATIONSHIP_TYPE_CONCEPT_MAP_B;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_RELATIONSHIP_TYPE_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_ATTRIB_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.IDENTIFIER;
import static org.openmrs.module.fgh.mpi.MpiConstants.NAME;
import static org.openmrs.module.fgh.mpi.MpiConstants.PERSON_UUID_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.UUID_PREFIX;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiUtils.executeQuery;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Location;
import org.openmrs.PersonAttributeType;
import org.openmrs.RelationshipType;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class })
public class FhirUtilsTest {
	
	@Mock
	private AdministrationService mockAdminService;
	
	@Mock
	private PersonService mockPersonService;
	
	@Mock
	private LocationService mockLocationService;
	
	private static final String TERMINOLOGY_SYSTEM = "http://test.com";
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		Whitebox.setInternalState(FhirUtils.class, "ATTR_TYPE_GP_ID_MAP", new HashMap(2));
		Whitebox.setInternalState(FhirUtils.class, "relationshipTypeSystem", (Object) null);
		Whitebox.setInternalState(FhirUtils.class, "uuidFhirRelationshipTypePersonAMap", (Object) null);
		Whitebox.setInternalState(FhirUtils.class, "uuidFhirRelationshipTypePersonBMap", (Object) null);
		when(Context.getAdministrationService()).thenReturn(mockAdminService);
		when(Context.getPersonService()).thenReturn(mockPersonService);
		when(Context.getLocationService()).thenReturn(mockLocationService);
		when(mockAdminService.getGlobalProperty(GP_PHONE_MOBILE)).thenReturn("test-mobile-uuid");
		when(mockAdminService.getGlobalProperty(GP_PHONE_HOME)).thenReturn("test-home-uuid");
		when(mockPersonService.getPersonAttributeTypeByUuid(anyString())).thenReturn(new PersonAttributeType(0));
	}
	
	@Test
	public void buildPatient_shouldBuildAPatientResource() {
		final String patientId = "1";
		final String birthDate = "1986-10-07";
		final boolean dead = false;
		final String patientUuid = "person-uuid";
		final boolean personVoided = false;
		final boolean patientVoided = false;
		List<Object> personDetails = asList("M", birthDate, dead, null, patientUuid, personVoided);
		final String identifier1 = "12345";
		final String idTypeUuid1 = "id-type-uuid-1";
		final String idUuid1 = "id-uuid-1";
		List<Object> id1 = asList(identifier1, idTypeUuid1, idUuid1);
		final String identifier2 = "qwerty";
		final String idTypeUuid2 = "id-type-uuid-2";
		final String idUuid2 = "id-uuid-2";
		List<Object> id2 = asList(identifier2, idTypeUuid2, idUuid2);
		List<List<Object>> ids = asList(id1, id2);
		when(executeQuery(FhirUtils.ID_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(ids);
		final String prefix1 = "Mr";
		final String givenName1 = "Horatio";
		final String middleName1 = "D";
		final String familyName1 = "HornBlower";
		final String nameUuid1 = "name-uuid-1";
		List<Object> name1 = asList(prefix1, givenName1, middleName1, familyName1, nameUuid1);
		final String prefix2 = "Miss";
		final String givenName2 = "John";
		final String middleName2 = "S";
		final String familyName2 = "Doe";
		final String nameUuid2 = "name-uuid-2";
		List<Object> name2 = asList(prefix2, givenName2, middleName2, familyName2, nameUuid2);
		List<List<Object>> names = asList(name1, name2);
		when(executeQuery(NAME_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(names);
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
		List<Object> personAddress1 = asList(line1Address1, line1Address2, line1Address3, line1Address5, line1Address6,
		    countyDistrict1, stateProvince1, country1, startDate1, endDate1, addressUuid1);
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
		List<Object> personAddress2 = asList(line2Address1, line2Address2, line2Address3, line2Address5, line2Address6,
		    countyDistrict2, stateProvince2, country2, startDate2, endDate2, addressUuid2);
		List<List<Object>> addresses = new ArrayList();
		addresses.add(personAddress1);
		addresses.add(personAddress2);
		when(executeQuery(FhirUtils.ADDRESS_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(addresses);
		
		final String mobile = "123-456-7890";
		final String attributeUuid1 = "attr-uuid-1";
		final Integer mobileAttrTypeId = 1;
		final String mobileAttrTypeUuid = "attr-type-uuid-1";
		List<Object> mobileAttr = asList(mobile, attributeUuid1);
		when(mockAdminService.getGlobalProperty(GP_PHONE_MOBILE)).thenReturn(mobileAttrTypeUuid);
		PersonAttributeType mobileAttrType = new PersonAttributeType(mobileAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(mobileAttrTypeUuid)).thenReturn(mobileAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, mobileAttrTypeId.toString())))
		            .thenReturn(singletonList(mobileAttr));
		final String home = "098-765-4321";
		final String attributeUuid2 = "attr-uuid-2";
		final Integer homeAttrTypeId = 2;
		final String homeAttrTypeUuid = "attr-type-uuid-2";
		List<Object> homePhoneAttr = asList(home, attributeUuid2);
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
		List<Object> healthCenter = asList(locationId.toString(), "attrib-uuid");
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
		
		Map<String, Object> resource = FhirUtils.buildPatient(patientId, patientVoided, personDetails, null);
		
		//System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resource));
		assertEquals(MpiConstants.PATIENT, resource.get(MpiConstants.FIELD_RESOURCE_TYPE));
		assertEquals(GENDER_MALE, resource.get(MpiConstants.FIELD_GENDER));
		assertEquals(!patientVoided, resource.get(MpiConstants.FIELD_ACTIVE));
		assertEquals(birthDate, resource.get(MpiConstants.FIELD_BIRTHDATE));
		assertEquals(dead, resource.get(MpiConstants.FIELD_DECEASED));
		assertNull(resource.get(MpiConstants.FIELD_DECEASED_DATE));
		
		List<Map> resourceIds = (List) resource.get(MpiConstants.FIELD_IDENTIFIER);
		assertEquals(3, resourceIds.size());
		assertEquals(MpiConstants.SOURCE_ID_URI, resourceIds.get(0).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(patientUuid, resourceIds.get(0).get(MpiConstants.FIELD_VALUE));
		assertEquals(MpiConstants.UUID_PREFIX + idTypeUuid1, resourceIds.get(1).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(identifier1, resourceIds.get(1).get(MpiConstants.FIELD_VALUE));
		assertEquals(idUuid1, resourceIds.get(1).get(FIELD_ID));
		assertEquals(MpiConstants.UUID_PREFIX + idTypeUuid2, resourceIds.get(2).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(identifier2, resourceIds.get(2).get(MpiConstants.FIELD_VALUE));
		assertEquals(idUuid2, resourceIds.get(2).get(FIELD_ID));
		
		List<Map> resourceNames = (List) resource.get(FIELD_NAME);
		assertEquals(2, resourceNames.size());
		assertEquals(MpiConstants.USE_OFFICIAL, resourceNames.get(0).get(FIELD_USE));
		assertEquals(prefix1, resourceNames.get(0).get(FIELD_PREFIX));
		assertEquals(nameUuid1, resourceNames.get(0).get(FIELD_ID));
		assertEquals(familyName1, resourceNames.get(0).get(MpiConstants.FIELD_FAMILY));
		List<Object> givenNames1 = (List) resourceNames.get(0).get(FIELD_GIVEN);
		assertEquals(givenName1, givenNames1.get(0));
		assertEquals(middleName1, givenNames1.get(1));
		assertNull(resourceNames.get(1).get(FIELD_USE));
		assertEquals(prefix2, resourceNames.get(1).get(FIELD_PREFIX));
		assertEquals(nameUuid2, resourceNames.get(1).get(FIELD_ID));
		assertEquals(familyName2, resourceNames.get(1).get(MpiConstants.FIELD_FAMILY));
		List<Object> givenNames2 = (List) resourceNames.get(1).get(FIELD_GIVEN);
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
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(startDate1)), period1.get(FIELD_START));
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(endDate1)), period1.get(FIELD_END));
		assertEquals(countyDistrict1, resourceAddresses.get(0).get(MpiConstants.FIELD_DISTRICT));
		assertEquals(stateProvince1, resourceAddresses.get(0).get(MpiConstants.FIELD_STATE));
		assertEquals(country1, resourceAddresses.get(0).get(MpiConstants.FIELD_COUNTRY));
		assertEquals(addressUuid1, resourceAddresses.get(0).get(FIELD_ID));
		List<Object> line2 = (List) resourceAddresses.get(1).get(MpiConstants.FIELD_LINE);
		assertEquals(line2Address2, line2.get(0));
		assertEquals(line2Address6, line2.get(1));
		assertEquals(line2Address5, line2.get(2));
		assertEquals(line2Address3, line2.get(3));
		assertEquals(line2Address1, line2.get(4));
		Map period2 = (Map) resourceAddresses.get(1).get(MpiConstants.FIELD_PERIOD);
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(startDate2)), period2.get(FIELD_START));
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(endDate2)), period2.get(FIELD_END));
		assertEquals(countyDistrict2, resourceAddresses.get(1).get(MpiConstants.FIELD_DISTRICT));
		assertEquals(stateProvince2, resourceAddresses.get(1).get(MpiConstants.FIELD_STATE));
		assertEquals(country2, resourceAddresses.get(1).get(MpiConstants.FIELD_COUNTRY));
		assertEquals(addressUuid2, resourceAddresses.get(1).get(FIELD_ID));
		
		List<Map> resourceTelecoms = (List) resource.get(MpiConstants.FIELD_TELECOM);
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(0).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.MOBILE, resourceTelecoms.get(0).get(FIELD_USE));
		assertEquals(mobile, resourceTelecoms.get(0).get(MpiConstants.FIELD_VALUE));
		assertEquals(attributeUuid1, resourceTelecoms.get(0).get(FIELD_ID));
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(1).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.HOME, resourceTelecoms.get(1).get(FIELD_USE));
		assertEquals(home, resourceTelecoms.get(1).get(MpiConstants.FIELD_VALUE));
		assertEquals(attributeUuid2, resourceTelecoms.get(1).get(FIELD_ID));
		
		List<Map> extension = (List) resource.get(MpiConstants.FIELD_EXTENSION);
		assertEquals(1, extension.size());
		assertEquals(HEALTH_CENTER_URL, extension.get(0).get(MpiConstants.FIELD_URL));
		extension = (List) extension.get(0).get(MpiConstants.FIELD_EXTENSION);
		assertEquals(IDENTIFIER, extension.get(0).get(MpiConstants.FIELD_URL));
		assertEquals(UUID_PREFIX + locationUuid, extension.get(0).get(MpiConstants.FIELD_VALUE_UUID));
		assertEquals(NAME, extension.get(1).get(MpiConstants.FIELD_URL));
		assertEquals(locationName, extension.get(1).get(MpiConstants.FIELD_VALUE_STR));
	}
	
	@Test
	public void buildPatient_shouldSetTheCorrectGender() {
		List<Object> personDetails = asList("M", null, false, null, null, false);
		assertEquals(GENDER_MALE, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		//Should be case insensitive
		personDetails = asList("m", null, false, null, null, false);
		assertEquals(GENDER_MALE, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		
		personDetails = asList("F", null, false, null, null, false);
		assertEquals(GENDER_FEMALE, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		
		personDetails = asList("f", null, false, null, null, false);
		assertEquals(GENDER_FEMALE, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		
		personDetails = asList("O", null, false, null, null, false);
		assertEquals(GENDER_OTHER, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		
		personDetails = asList("o", null, false, null, null, false);
		assertEquals(GENDER_OTHER, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		
		personDetails = asList(null, null, false, null, null, false);
		assertEquals(GENDER_UNKNOWN, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		
		personDetails = asList("", null, false, null, null, false);
		assertEquals(GENDER_UNKNOWN, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
		
		personDetails = asList(" ", null, false, null, null, false);
		assertEquals(GENDER_UNKNOWN, FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_GENDER));
	}
	
	@Test
	public void buildPatient_shouldFailForAnInvalidGender() {
		expectedException.expect(APIException.class);
		final String gender = "Y";
		expectedException.expectMessage(equalTo("Don't know how to represent in fhir gender value: " + gender));
		FhirUtils.buildPatient("1", false, asList(gender, null, false, null, null, false), null);
	}
	
	@Test
	public void buildPatient_shouldSetDeathDateIfSpecifiedForADeadPatient() throws Exception {
		final String deathDate = "2020-12-12 13:00:00";
		List<Object> personDetails = asList(null, null, true, deathDate, null, false);
		
		Map<String, Object> resource = FhirUtils.buildPatient("1", false, personDetails, null);
		
		assertNull(resource.get(MpiConstants.FIELD_DECEASED));
		Date expectedDate = Timestamp.valueOf(deathDate);
		assertEquals(DATETIME_FORMATTER.format(expectedDate), resource.get(MpiConstants.FIELD_DECEASED_DATE));
	}
	
	@Test
	public void buildPatient_shouldNotSetDeathDateIfNotSpecifiedForADeadPatient() {
		List<Object> personDetails = asList(null, null, true, null, null, false);
		
		Map<String, Object> resource = FhirUtils.buildPatient("1", false, personDetails, null);
		
		assertNull(resource.get(MpiConstants.FIELD_DECEASED_DATE));
		assertEquals(true, resource.get(MpiConstants.FIELD_DECEASED));
	}
	
	@Test
	public void buildPatient_shouldOmitHealthCenterExtensionIfThePatientHasNone() {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		assertNull(FhirUtils.buildPatient("1", false, personDetails, null).get(FIELD_EXTENSION));
	}
	
	@Test
	public void buildPatient_shouldReplaceIdentifierWithNullValuesInTheMpiThatDoNotExistInOpenmrs() {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final String patientId = "1";
		final String identifier = "12345";
		final String idTypeUuid = "id-type-uuid-1";
		final String idUuid1 = "id-uuid-1";
		List<List<Object>> ids = asList(asList(identifier, idTypeUuid, idUuid1));
		when(executeQuery(FhirUtils.ID_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(ids);
		Map<String, Object> mpiPatient = singletonMap(IDENTIFIER, asList(null, null, null, null));
		
		Map<String, Object> res = FhirUtils.buildPatient(patientId, false, personDetails, mpiPatient);
		
		List identifiers = (List) res.get(IDENTIFIER);
		assertEquals(4, identifiers.size());
		assertNotNull(identifiers.get(0));
		assertNotNull(identifiers.get(1));
		assertNull(identifiers.get(2));
		assertNull(identifiers.get(3));
	}
	
	@Test
	public void buildPatient_shouldReplaceNamesWithNullValuesInTheMpiThatDoNotExistInOpenmrs() {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final String patientId = "1";
		final String givenName = "John";
		final String familyName = "Doe";
		List<List<Object>> ids = asList(asList(null, givenName, null, familyName, null));
		when(executeQuery(NAME_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(ids);
		Map<String, Object> mpiPatient = singletonMap(FIELD_NAME, asList(null, null, null));
		
		Map<String, Object> res = FhirUtils.buildPatient(patientId, false, personDetails, mpiPatient);
		
		List names = (List) res.get(FIELD_NAME);
		assertEquals(3, names.size());
		assertNotNull(names.get(0));
		assertNull(names.get(1));
		assertNull(names.get(2));
	}
	
	@Test
	public void buildPatient_shouldReplaceAddressesWithNullValuesInTheMpiThatDoNotExistInOpenmrs() {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final String patientId = "1";
		final String countyDistrict = "Test";
		List<List<Object>> ids = asList(asList(null, null, null, null, null, countyDistrict, null, null, null, null, null));
		when(executeQuery(FhirUtils.ADDRESS_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(ids);
		Map<String, Object> mpiPatient = singletonMap(FIELD_ADDRESS, asList(null, null, null));
		
		Map<String, Object> res = FhirUtils.buildPatient(patientId, false, personDetails, mpiPatient);
		
		List addresses = (List) res.get(FIELD_ADDRESS);
		assertEquals(3, addresses.size());
		assertNotNull(addresses.get(0));
		assertNull(addresses.get(1));
		assertNull(addresses.get(2));
	}
	
	@Test
	public void buildPatient_shouldReplacePhonesWithNullValuesInTheMpiThatDoNotExistInOpenmrs() {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final String patientId = "1";
		final String mobile = "123-456-7890";
		final String attributeUuid = "attr-uuid-1";
		final Integer mobileAttrTypeId = 1;
		final String mobileAttrTypeUuid = "attr-type-uuid-1";
		List<Object> mobileAttr = asList(mobile, attributeUuid);
		when(mockAdminService.getGlobalProperty(GP_PHONE_MOBILE)).thenReturn(mobileAttrTypeUuid);
		PersonAttributeType mobileAttrType = new PersonAttributeType(mobileAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(mobileAttrTypeUuid)).thenReturn(mobileAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, mobileAttrTypeId.toString())))
		            .thenReturn(singletonList(mobileAttr));
		Map<String, Object> mpiPatient = singletonMap(FIELD_TELECOM, asList(null, null, null));
		
		Map<String, Object> res = FhirUtils.buildPatient(patientId, false, personDetails, mpiPatient);
		
		List phones = (List) res.get(FIELD_TELECOM);
		assertEquals(3, phones.size());
		assertNotNull(phones.get(0));
		assertNull(phones.get(1));
		assertNull(phones.get(2));
	}
	
	@Test
	public void buildPatient_shouldReplaceHealthCenterWithNullValuesInTheMpiIfItDoNotExistInOpenmrs() {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final String patientId = "1";
		final Integer locationId = 1;
		final String locationUuid = "location-uuid";
		final String locationName = "Location";
		final Integer healthCtrAttrTypeId = 6;
		PersonAttributeType healthCenterAttrType = new PersonAttributeType(healthCtrAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(HEALTH_CENTER_ATTRIB_TYPE_UUID))
		        .thenReturn(healthCenterAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, healthCtrAttrTypeId.toString())))
		            .thenReturn(emptyList());
		Location location = new Location(locationId);
		location.setName(locationName);
		location.setUuid(locationUuid);
		when(mockLocationService.getLocation(locationId)).thenReturn(location);
		Map<String, Object> mpiPatient = singletonMap(FIELD_EXTENSION, asList(emptyMap()));
		
		Map<String, Object> res = FhirUtils.buildPatient(patientId, false, personDetails, mpiPatient);
		
		List<Map> extension = (List) res.get(MpiConstants.FIELD_EXTENSION);
		assertEquals(1, extension.size());
		assertEquals(HEALTH_CENTER_URL, extension.get(0).get(MpiConstants.FIELD_URL));
		extension = (List) extension.get(0).get(MpiConstants.FIELD_EXTENSION);
		assertEquals(IDENTIFIER, extension.get(0).get(MpiConstants.FIELD_URL));
		assertNull(extension.get(0).get(MpiConstants.FIELD_VALUE_UUID));
		assertEquals(NAME, extension.get(1).get(MpiConstants.FIELD_URL));
		assertNull(extension.get(1).get(MpiConstants.FIELD_VALUE_STR));
	}
	
	@Test
	public void buildPatient_shouldIncludeRelationshipsInThePatientResource() throws IOException {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final Integer patientId = 1;
		final Integer motherPersonId = 2;
		final String motherUuid = "mother-uuid";
		final String motherGivenName = "Mary";
		final String motherFamilyName = "Jane";
		final String motherNameUuid = "mother-name-uuid";
		final String motherRelationshipUuid = "mother-relationship-uuid";
		final String motherRelationshipTypeUuid = "mother-relationship-type-uuid";
		final String motherRelationshipTypeCode = "M";
		final String motherRelationshipTypeDisplay = "Mother";
		final String motherRelationshipTypePersonB = "Biological Mother";
		List<Object> motherName = asList(null, motherGivenName, null, motherFamilyName, motherNameUuid);
		when(executeQuery((NAME_QUERY + " LIMIT 1").replace(ID_PLACEHOLDER, motherPersonId.toString())))
		        .thenReturn(asList(motherName));
		final String motherLine1Address2 = "123";
		final String motherLine1Address6 = "Ocean";
		final String motherLine1Address5 = "Dr";
		final String motherCountyDistrict = "Travis";
		final String motherStateProvince = "Texas";
		final String motherCountry = "US";
		final String motherAddressUuid = "address-uuid-1";
		List<Object> motherAddress = asList(null, motherLine1Address2, null, motherLine1Address5, motherLine1Address6,
		    motherCountyDistrict, motherStateProvince, motherCountry, null, null, motherAddressUuid);
		when(executeQuery((ADDRESS_QUERY + " LIMIT 1").replace(ID_PLACEHOLDER, motherPersonId.toString())))
		        .thenReturn(asList(motherAddress));
		final String motherMobile = "333-456-4444";
		final String attributeUuid1 = "attr-uuid-1";
		final Integer mobileAttrTypeId = 1;
		final String mobileAttrTypeUuid = "attr-type-uuid-1";
		List<Object> mobileAttr = asList(motherMobile, attributeUuid1);
		when(mockAdminService.getGlobalProperty(GP_PHONE_MOBILE)).thenReturn(mobileAttrTypeUuid);
		PersonAttributeType mobileAttrType = new PersonAttributeType(mobileAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(mobileAttrTypeUuid)).thenReturn(mobileAttrType);
		when(executeQuery(ATTR_QUERY.replace(ID_PLACEHOLDER, motherPersonId.toString()).replace(ATTR_TYPE_ID_PLACEHOLDER,
		    mobileAttrTypeId.toString()))).thenReturn(singletonList(mobileAttr));
		final Integer husbandPersonId = 3;
		final String husbandUuid = "husband-uuid";
		final String husbandGivenName = "Horatio";
		final String husbandFamilyName = "Hornblower";
		final String husbandNameUuid = "husband-name-uuid";
		final String husbandRelationshipUuid = "husband-relationship-uuid";
		final String husbandRelationshipTypeUuid = "husband-relationship-type-uuid";
		final String husbandRelationshipTypeCode = "H";
		final String husbandRelationshipTypeDisplay = "Husband";
		final String husbandRelationshipTypePersonA = "Legal Husband";
		List<Object> husbandName = asList(null, husbandGivenName, null, husbandFamilyName, husbandNameUuid);
		when(executeQuery((NAME_QUERY + " LIMIT 1").replace(ID_PLACEHOLDER, husbandPersonId.toString())))
		        .thenReturn(asList(husbandName));
		List<Object> motherDetails = asList("F", motherUuid);
		when(executeQuery(CONTACT_PERSON_QUERY.replace(ID_PLACEHOLDER, motherPersonId.toString())))
		        .thenReturn(asList(motherDetails));
		List<Object> husbandDetails = asList("M", husbandUuid);
		when(executeQuery(CONTACT_PERSON_QUERY.replace(ID_PLACEHOLDER, husbandPersonId.toString())))
		        .thenReturn(asList(husbandDetails));
		List<Object> motherRelationship = asList(patientId, motherPersonId, null, null, motherRelationshipUuid,
		    motherRelationshipTypeUuid);
		final String startDate = "2021-01-01 00:00:00";
		final String endDate = "2021-12-31 00:00:00";
		List<Object> husbandRelationship = asList(husbandPersonId, patientId, startDate, endDate, husbandRelationshipUuid,
		    husbandRelationshipTypeUuid);
		List<List<Object>> relationships = asList(motherRelationship, husbandRelationship);
		when(executeQuery(RELATIONSHIP_QUERY.replace(ID_PLACEHOLDER, patientId.toString()))).thenReturn(relationships);
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_SYSTEM)).thenReturn(TERMINOLOGY_SYSTEM);
		final String motherMap = motherRelationshipTypeUuid + ":" + motherRelationshipTypeCode + ":"
		        + motherRelationshipTypeDisplay;
		final String husbandMap = husbandRelationshipTypeUuid + ":" + husbandRelationshipTypeCode + ":"
		        + husbandRelationshipTypeDisplay;
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_CONCEPT_MAP_B)).thenReturn(motherMap);
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_CONCEPT_MAP_A)).thenReturn(husbandMap);
		RelationshipType motherRelationshipType = new RelationshipType();
		motherRelationshipType.setbIsToA(motherRelationshipTypePersonB);
		when(mockPersonService.getRelationshipTypeByUuid(motherRelationshipTypeUuid)).thenReturn(motherRelationshipType);
		RelationshipType husbandRelationshipType = new RelationshipType();
		husbandRelationshipType.setaIsToB(husbandRelationshipTypePersonA);
		when(mockPersonService.getRelationshipTypeByUuid(husbandRelationshipTypeUuid)).thenReturn(husbandRelationshipType);
		
		Map<String, Object> res = FhirUtils.buildPatient(patientId.toString(), false, personDetails, null);
		
		//System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(res));
		List<Map> contacts = (List) res.get(MpiConstants.FIELD_CONTACT);
		assertEquals(2, contacts.size());
		Map mother = contacts.get(0);
		assertEquals(motherRelationshipUuid, mother.get(FIELD_ID));
		assertEquals(GENDER_FEMALE, mother.get(FIELD_GENDER));
		Map motherUuidExt = (Map) ((List) mother.get(FIELD_EXTENSION)).get(0);
		assertEquals(PERSON_UUID_URL, motherUuidExt.get(FIELD_URL));
		assertEquals(UUID_PREFIX + motherUuid, motherUuidExt.get(FIELD_VALUE_UUID));
		Map motherNameResource = (Map) mother.get(FIELD_NAME);
		assertEquals(MpiConstants.USE_OFFICIAL, motherNameResource.get(FIELD_USE));
		assertEquals(motherNameUuid, motherNameResource.get(FIELD_ID));
		assertEquals(motherFamilyName, motherNameResource.get(MpiConstants.FIELD_FAMILY));
		List<Object> motherGivenNames = (List) motherNameResource.get(FIELD_GIVEN);
		assertEquals(motherGivenName, motherGivenNames.get(0));
		Map motherPeriod = (Map) mother.get(MpiConstants.FIELD_PERIOD);
		assertNull(motherPeriod.get(FIELD_START));
		assertNull(motherPeriod.get(FIELD_END));
		Map motherContactType = (Map) mother.get(FIELD_RELATIONSHIP);
		Map motherContactCoding = (Map) ((List) motherContactType.get(FIELD_CODING)).get(0);
		assertEquals(TERMINOLOGY_SYSTEM, motherContactCoding.get(FIELD_SYSTEM));
		assertEquals(motherRelationshipTypeCode, motherContactCoding.get(FIELD_CODE));
		assertEquals(motherRelationshipTypeDisplay, motherContactCoding.get(FIELD_DISPLAY));
		assertEquals(motherRelationshipTypePersonB, motherContactType.get(FIELD_TEXT));
		
		Map motherAddressResource = (Map) mother.get(MpiConstants.FIELD_ADDRESS);
		List<Object> motherLine1 = (List) motherAddressResource.get(MpiConstants.FIELD_LINE);
		assertEquals(motherLine1Address2, motherLine1.get(0));
		assertEquals(motherLine1Address6, motherLine1.get(1));
		assertEquals(motherLine1Address5, motherLine1.get(2));
		assertEquals(motherCountyDistrict, motherAddressResource.get(MpiConstants.FIELD_DISTRICT));
		assertEquals(motherStateProvince, motherAddressResource.get(MpiConstants.FIELD_STATE));
		assertEquals(motherCountry, motherAddressResource.get(MpiConstants.FIELD_COUNTRY));
		assertEquals(motherAddressUuid, motherAddressResource.get(FIELD_ID));
		
		List<Map> motherTelecoms = (List) mother.get(MpiConstants.FIELD_TELECOM);
		assertEquals(MpiConstants.PHONE, motherTelecoms.get(0).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.MOBILE, motherTelecoms.get(0).get(FIELD_USE));
		assertEquals(motherMobile, motherTelecoms.get(0).get(MpiConstants.FIELD_VALUE));
		assertEquals(attributeUuid1, motherTelecoms.get(0).get(FIELD_ID));
		
		Map husband = contacts.get(1);
		assertEquals(husbandRelationshipUuid, husband.get(FIELD_ID));
		assertEquals(GENDER_MALE, husband.get(FIELD_GENDER));
		Map husbandUuidExt = (Map) ((List) husband.get(FIELD_EXTENSION)).get(0);
		assertEquals(PERSON_UUID_URL, husbandUuidExt.get(FIELD_URL));
		assertEquals(UUID_PREFIX + husbandUuid, husbandUuidExt.get(FIELD_VALUE_UUID));
		assertEquals(husbandRelationshipUuid, contacts.get(1).get(FIELD_ID));
		Map husbandNameResource = (Map) husband.get(FIELD_NAME);
		assertEquals(MpiConstants.USE_OFFICIAL, husbandNameResource.get(FIELD_USE));
		assertEquals(husbandNameUuid, husbandNameResource.get(FIELD_ID));
		assertEquals(husbandFamilyName, husbandNameResource.get(MpiConstants.FIELD_FAMILY));
		List<Object> husbandGivenNames = (List) husbandNameResource.get(FIELD_GIVEN);
		assertEquals(husbandGivenName, husbandGivenNames.get(0));
		Map husbandPeriod = (Map) contacts.get(1).get(MpiConstants.FIELD_PERIOD);
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(startDate)), husbandPeriod.get(FIELD_START));
		assertEquals(DATETIME_FORMATTER.format(Timestamp.valueOf(endDate)), husbandPeriod.get(FIELD_END));
		Map husbandContactType = (Map) husband.get(FIELD_RELATIONSHIP);
		Map husbandContactCoding = (Map) ((List) husbandContactType.get(FIELD_CODING)).get(0);
		assertEquals(TERMINOLOGY_SYSTEM, husbandContactCoding.get(FIELD_SYSTEM));
		assertEquals(husbandRelationshipTypeCode, husbandContactCoding.get(FIELD_CODE));
		assertEquals(husbandRelationshipTypeDisplay, husbandContactCoding.get(FIELD_DISPLAY));
		assertEquals(husbandRelationshipTypePersonA, husbandContactType.get(FIELD_TEXT));
	}
	
	@Test
	public void buildPatient_shouldFailIfRelationshipTypeSystemGlobalPropertyIsNotSet() {
		final String patientId = "1";
		when(executeQuery(RELATIONSHIP_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(asList(emptyList()));
		expectedException.expect(APIException.class);
		expectedException
		        .expectMessage(equalTo("No value set for the global property named: " + GP_RELATIONSHIP_TYPE_SYSTEM));
		FhirUtils.buildPatient(patientId, false, asList(null, null, false, null, null, false), null);
	}
	
	@Test
	public void buildPatient_shouldFailIfNoConceptIsMappedToTheRelationshipType() {
		final String patientId = "1";
		final String uuid = "mother-relationship-type-uuid";
		List<Object> relationship = asList(patientId, null, null, null, null, uuid);
		when(executeQuery(RELATIONSHIP_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(asList(relationship));
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_SYSTEM)).thenReturn(TERMINOLOGY_SYSTEM);
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_CONCEPT_MAP_B)).thenReturn(uuid + ":M:Mother");
		expectedException.expect(APIException.class);
		expectedException.expectMessage(equalTo("No relationship type found with uuid: " + uuid));
		FhirUtils.buildPatient(patientId, false, asList(null, null, false, null, null, false), null);
	}
	
	@Test
	public void buildPatient_shouldFailIfNoConceptMapForPersonBIsFoundForTheRelationshipTypeMatchingTheMappedUuid() {
		final String patientId = "1";
		final String uuid = "mother-relationship-type-uuid";
		List<Object> relationship = asList(patientId, null, null, null, null, uuid);
		when(executeQuery(RELATIONSHIP_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(asList(relationship));
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_SYSTEM)).thenReturn(TERMINOLOGY_SYSTEM);
		expectedException.expect(APIException.class);
		expectedException
		        .expectMessage(equalTo("No concept mapped to person B of the relationship type with uuid: " + uuid));
		FhirUtils.buildPatient(patientId, false, asList(null, null, false, null, null, false), null);
	}
	
	@Test
	public void buildPatient_shouldFailIfNoConceptMapForPersonABIsFoundForTheRelationshipTypeMatchingTheMappedUuid() {
		final String patientId = "1";
		final String uuid = "mother-relationship-type-uuid";
		List<Object> relationship = asList(101, patientId, null, null, null, uuid);
		when(executeQuery(RELATIONSHIP_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(asList(relationship));
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_SYSTEM)).thenReturn(TERMINOLOGY_SYSTEM);
		expectedException.expect(APIException.class);
		expectedException
		        .expectMessage(equalTo("No concept mapped to person A of the relationship type with uuid: " + uuid));
		FhirUtils.buildPatient(patientId, false, asList(null, null, false, null, null, false), null);
	}
	
	@Test
	public void buildPatient_shouldReplaceContactsWithNullValuesInTheMpiThatDoNotExistInOpenmrs() {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final Integer patientId = 1;
		final Integer motherPersonId = 2;
		final String motherRelationshipUuid = "mother-relationship-uuid";
		final String motherRelationshipTypeUuid = "mother-relationship-type-uuid";
		final String motherRelationshipTypeCode = "M";
		final String motherRelationshipTypeDisplay = "Mother";
		final String motherRelationshipTypeText = "Biological Mother";
		List<Object> motherName = asList(null, "Mary", null, "Jane", "mother-name-uuid");
		when(executeQuery((NAME_QUERY + " LIMIT 1").replace(ID_PLACEHOLDER, motherPersonId.toString())))
		        .thenReturn(asList(motherName));
		List<Object> motherDetails = asList("F", "mother-uuid");
		when(executeQuery(CONTACT_PERSON_QUERY.replace(ID_PLACEHOLDER, motherPersonId.toString())))
		        .thenReturn(asList(motherDetails));
		List<Object> motherRelationship = asList(patientId, motherPersonId, null, null, motherRelationshipUuid,
		    motherRelationshipTypeUuid);
		List<List<Object>> relationships = asList(motherRelationship);
		when(executeQuery(RELATIONSHIP_QUERY.replace(ID_PLACEHOLDER, patientId.toString()))).thenReturn(relationships);
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_SYSTEM)).thenReturn(TERMINOLOGY_SYSTEM);
		final String motherMap = motherRelationshipTypeUuid + ":" + motherRelationshipTypeCode + ":"
		        + motherRelationshipTypeDisplay;
		when(mockAdminService.getGlobalProperty(GP_RELATIONSHIP_TYPE_CONCEPT_MAP_B)).thenReturn(motherMap);
		RelationshipType motherRelationshipType = new RelationshipType();
		motherRelationshipType.setName(motherRelationshipTypeText);
		when(mockPersonService.getRelationshipTypeByUuid(motherRelationshipTypeUuid)).thenReturn(motherRelationshipType);
		Map<String, Object> mpiPatient = singletonMap(FIELD_CONTACT, asList(emptyMap(), emptyMap(), emptyMap()));
		
		Map<String, Object> res = FhirUtils.buildPatient(patientId.toString(), false, personDetails, mpiPatient);
		
		List<Map> contacts = (List) res.get(MpiConstants.FIELD_CONTACT);
		assertEquals(3, contacts.size());
		assertNotNull(contacts.get(0));
		assertNull(contacts.get(1));
		assertNull(contacts.get(2));
	}
	
}
