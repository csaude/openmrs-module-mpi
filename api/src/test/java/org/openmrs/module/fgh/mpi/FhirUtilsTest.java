package org.openmrs.module.fgh.mpi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fgh.mpi.FhirUtils.ATTR_QUERY;
import static org.openmrs.module.fgh.mpi.FhirUtils.ATTR_TYPE_ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.FhirUtils.NAME_QUERY;
import static org.openmrs.module.fgh.mpi.MpiConstants.DATETIME_FORMATTER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ADDRESS;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_END;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_EXTENSION;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_GENDER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_GIVEN;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_NAME;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_PREFIX;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_START;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_TELECOM;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_USE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_FEMALE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_MALE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_OTHER;
import static org.openmrs.module.fgh.mpi.MpiConstants.GENDER_UNKNOWN;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_HEALTH_FACILITY_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_ID_TYPE_SYSTEM_MAP;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_PHONE_HOME;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_PHONE_MOBILE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_EVENT_URI;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_ATTRIB_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.IDENTIFIER;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiUtils.executeQuery;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fgh.mpi.api.MpiService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, MpiContext.class })
public class FhirUtilsTest {
	
	@Mock
	private PersonService mockPersonService;
	
	@Mock
	private PatientService mockPatientService;
	
	@Mock
	private LocationService mockLocationService;
	
	@Mock
	private MpiService mockMpiService;
	
	@Mock
	private MpiContext mockMpiContext;
	
	private static final String UUID_SYSTEM = "http://test.openmrs.id/uuid";
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	private static final String MESSAGE_HEADER_REFERENCE = "metadata.epts.e-saude.net/bundle";
	
	private static final String MESSAGE_HEADER_EVENT_URI = "urn:ihe:iti:pmir:2019:patient-feed";
	
	private static final String NAME = "JAVA 8";
	
	private static final String PHONE = "+12345678909";
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(MpiContext.class);
		Whitebox.setInternalState(FhirUtils.class, "ATTR_TYPE_GP_ID_MAP", new HashMap(2));
		Whitebox.setInternalState(FhirUtils.class, "idSystemMap", (Object) null);
		Whitebox.setInternalState(FhirUtils.class, "openmrsUuidSystem", (Object) null);
		when(Context.getPersonService()).thenReturn(mockPersonService);
		when(Context.getPatientService()).thenReturn(mockPatientService);
		when(Context.getLocationService()).thenReturn(mockLocationService);
		when(Context.getService(MpiService.class)).thenReturn(mockMpiService);
		when(MpiUtils.getGlobalPropertyValue(GP_PHONE_MOBILE)).thenReturn("test-mobile-uuid");
		when(MpiUtils.getGlobalPropertyValue(GP_PHONE_HOME)).thenReturn("test-home-uuid");
		when(mockPersonService.getPersonAttributeTypeByUuid(anyString())).thenReturn(new PersonAttributeType(0));
		when(MpiUtils.getGlobalPropertyValue(GP_UUID_SYSTEM)).thenReturn(UUID_SYSTEM);
		when(MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE)).thenReturn(MESSAGE_HEADER_REFERENCE);
		when(MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_EVENT_URI)).thenReturn(MESSAGE_HEADER_EVENT_URI);
		Whitebox.setInternalState(MpiContext.class, MpiContext.class, mockMpiContext);
		when(mockMpiContext.getMpiSystem()).thenReturn(MpiSystemType.SANTEMPI);
	}
	
	@Test
	public void buildPatient_shouldBuildAPatientResource() throws Exception {
		final String patientId = "1";
		final String birthDate = "1986-10-07";
		final boolean dead = false;
		final String patientUuid = "person-uuid";
		final boolean personVoided = false;
		final boolean patientVoided = false;
		List<Object> personDetails = asList("M", birthDate, dead, null, patientUuid, personVoided);
		final String identifier1 = "12345";
		final String idTypeName1 = "id-type-name-1";
		final String idTypeUuid1 = "id-type-uuid-1";
		final String idUuid1 = "id-uuid-1";
		final String idTypeSystem1 = "id-type-system-1";
		List<Object> id1 = asList(identifier1, idTypeUuid1, idUuid1);
		final String identifier2 = "qwerty";
		final String idTypeName2 = "id-type-name-2";
		final String idTypeUuid2 = "id-type-uuid-2";
		final String idUuid2 = "id-uuid-2";
		final String idTypeSystem2 = "id-type-system-2";
		List<Object> id2 = asList(identifier2, idTypeUuid2, idUuid2);
		List<List<Object>> ids = asList(id1, id2);
		final String idType1SystemMap = idTypeUuid1 + "^" + idTypeSystem1;
		final String idType2SystemMap = idTypeUuid2 + "^" + idTypeSystem2;
		when(MpiUtils.getGlobalPropertyValue(GP_ID_TYPE_SYSTEM_MAP)).thenReturn(idType1SystemMap + "," + idType2SystemMap);
		when(MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_EVENT_URI)).thenReturn(MESSAGE_HEADER_EVENT_URI);
		when(MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE)).thenReturn(MESSAGE_HEADER_REFERENCE);
		PatientIdentifierType idType1 = new PatientIdentifierType();
		idType1.setName(idTypeName1);
		when(mockPatientService.getPatientIdentifierTypeByUuid(idTypeUuid1)).thenReturn(idType1);
		PatientIdentifierType idType2 = new PatientIdentifierType();
		idType2.setName(idTypeName2);
		when(mockPatientService.getPatientIdentifierTypeByUuid(idTypeUuid2)).thenReturn(idType2);
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
		when(MpiUtils.getGlobalPropertyValue(GP_PHONE_MOBILE)).thenReturn(mobileAttrTypeUuid);
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
		when(MpiUtils.getGlobalPropertyValue(MpiConstants.GP_PHONE_HOME)).thenReturn(homeAttrTypeUuid);
		PersonAttributeType homeAttrType = new PersonAttributeType(homeAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(homeAttrTypeUuid)).thenReturn(homeAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, homeAttrTypeId.toString())))
		            .thenReturn(singletonList(homePhoneAttr));
		
		final String facilityLocUuid = "facility-uuid";
		final String facilityLocName = "facility-name";
		final String facilityIdSystem = "facility-id-system-uri";
		Location oldestLoc = new Location();
		oldestLoc.setUuid(facilityLocUuid);
		oldestLoc.setName(facilityLocName);
		Patient mockPatient = Mockito.mock(Patient.class);
		when(mockPatientService.getPatient(Integer.valueOf(patientId))).thenReturn(mockPatient);
		when(mockMpiService.getHealthFacility(mockPatient)).thenReturn(oldestLoc);
		when(MpiUtils.getGlobalPropertyValue(GP_HEALTH_FACILITY_SYSTEM)).thenReturn(facilityIdSystem);
		
		Map<String, Object> resource = FhirUtils.buildPatient(patientId, patientVoided, personDetails, null);
		
		System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resource));
		assertEquals(MpiConstants.PATIENT, resource.get(MpiConstants.FIELD_RESOURCE_TYPE));
		assertEquals(GENDER_MALE, resource.get(MpiConstants.FIELD_GENDER));
		assertEquals(!patientVoided, resource.get(MpiConstants.FIELD_ACTIVE));
		assertEquals(birthDate, resource.get(MpiConstants.FIELD_BIRTHDATE));
		assertEquals(dead, resource.get(MpiConstants.FIELD_DECEASED));
		assertNull(resource.get(MpiConstants.FIELD_DECEASED_DATE));
		
		List<Map> resourceIds = (List) resource.get(MpiConstants.FIELD_IDENTIFIER);
		assertEquals(4, resourceIds.size());
		assertEquals(UUID_SYSTEM, resourceIds.get(0).get(FIELD_SYSTEM));
		assertEquals(patientUuid, resourceIds.get(0).get(MpiConstants.FIELD_VALUE));
		assertEquals(idTypeSystem1, resourceIds.get(1).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(identifier1, resourceIds.get(1).get(MpiConstants.FIELD_VALUE));
		assertEquals(idUuid1, resourceIds.get(1).get(FIELD_ID));
		assertEquals(idTypeSystem2, resourceIds.get(2).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(identifier2, resourceIds.get(2).get(MpiConstants.FIELD_VALUE));
		assertEquals(idUuid2, resourceIds.get(2).get(FIELD_ID));
		assertEquals(facilityIdSystem, resourceIds.get(3).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(facilityLocName, resourceIds.get(3).get(MpiConstants.FIELD_VALUE));
		assertEquals(facilityLocUuid, resourceIds.get(3).get(FIELD_ID));
		
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
		final String idTypeUuid = "id-type-uuid";
		final String idUuid = "id-uuid";
		final String idTypeSystem = "id-type-system";
		List<List<Object>> ids = asList(asList(identifier, idTypeUuid, idUuid));
		when(executeQuery(FhirUtils.ID_QUERY.replace(ID_PLACEHOLDER, patientId))).thenReturn(ids);
		Map<String, Object> mpiPatient = singletonMap(IDENTIFIER, asList(null, null, null, null));
		when(mockPatientService.getPatientIdentifierTypeByUuid(idTypeUuid)).thenReturn(new PatientIdentifierType());
		when(MpiUtils.getGlobalPropertyValue(GP_ID_TYPE_SYSTEM_MAP)).thenReturn(idTypeUuid + "^" + idTypeSystem);
		
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
		when(MpiUtils.getGlobalPropertyValue(GP_PHONE_MOBILE)).thenReturn(mobileAttrTypeUuid);
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
	
	@Ignore
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
		extension = (List) extension.get(0).get(MpiConstants.FIELD_EXTENSION);
		assertEquals(IDENTIFIER, extension.get(0).get(MpiConstants.FIELD_URL));
		assertNull(extension.get(0).get(MpiConstants.FIELD_VALUE_UUID));
		assertEquals(NAME, extension.get(1).get(MpiConstants.FIELD_URL));
		assertNull(extension.get(1).get(MpiConstants.FIELD_VALUE_STR));
	}
	
	@Test
	public void buildPatient_shouldIncludeAllMobileAndHomePhones() throws IOException {
		List<Object> personDetails = asList(null, null, false, null, null, false);
		final String patientId = "1";
		final String mobile1 = "123-456-7890";
		final String mobile2 = "123-456-7891";
		final String mobileAttributeUuid1 = "mobile-attr-uuid-1";
		final String mobileAttributeUuid2 = "mobile-attr-uuid-2";
		final Integer mobileAttrTypeId = 1;
		final String mobileAttrTypeUuid = "mobile-attr-type-uuid";
		List<Object> mobileAttr1 = asList(mobile1, mobileAttributeUuid1);
		List<Object> mobileAttr2 = asList(mobile2, mobileAttributeUuid2);
		when(MpiUtils.getGlobalPropertyValue(GP_PHONE_MOBILE)).thenReturn(mobileAttrTypeUuid);
		PersonAttributeType mobileAttrType = new PersonAttributeType(mobileAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(mobileAttrTypeUuid)).thenReturn(mobileAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, mobileAttrTypeId.toString())))
		            .thenReturn(asList(mobileAttr1, mobileAttr2));
		final String home1 = "098-765-4321";
		final String home2 = "098-765-4322";
		final String homeAttributeUuid1 = "home-attr-uuid-1";
		final String homeAttributeUuid2 = "home-attr-uuid-2";
		final Integer homeAttrTypeId = 2;
		final String homeAttrTypeUuid = "home-attr-type-uuid";
		List<Object> homePhoneAttr1 = asList(home1, homeAttributeUuid1);
		List<Object> homePhoneAttr2 = asList(home2, homeAttributeUuid2);
		when(MpiUtils.getGlobalPropertyValue(MpiConstants.GP_PHONE_HOME)).thenReturn(homeAttrTypeUuid);
		PersonAttributeType homeAttrType = new PersonAttributeType(homeAttrTypeId);
		when(mockPersonService.getPersonAttributeTypeByUuid(homeAttrTypeUuid)).thenReturn(homeAttrType);
		when(executeQuery(
		    ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, homeAttrTypeId.toString())))
		            .thenReturn(asList(homePhoneAttr1, homePhoneAttr2));
		
		Map<String, Object> resource = FhirUtils.buildPatient(patientId, false, personDetails, null);
		
		System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resource));
		
		List<Map> resourceTelecoms = (List) resource.get(MpiConstants.FIELD_TELECOM);
		assertEquals(4, resourceTelecoms.size());
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(0).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.MOBILE, resourceTelecoms.get(0).get(FIELD_USE));
		assertEquals(mobile1, resourceTelecoms.get(0).get(MpiConstants.FIELD_VALUE));
		assertEquals(mobileAttributeUuid1, resourceTelecoms.get(0).get(FIELD_ID));
		
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(1).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.MOBILE, resourceTelecoms.get(1).get(FIELD_USE));
		assertEquals(mobile2, resourceTelecoms.get(1).get(MpiConstants.FIELD_VALUE));
		assertEquals(mobileAttributeUuid2, resourceTelecoms.get(1).get(FIELD_ID));
		
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(2).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.HOME, resourceTelecoms.get(2).get(FIELD_USE));
		assertEquals(home1, resourceTelecoms.get(2).get(MpiConstants.FIELD_VALUE));
		assertEquals(homeAttributeUuid1, resourceTelecoms.get(2).get(FIELD_ID));
		
		assertEquals(MpiConstants.PHONE, resourceTelecoms.get(3).get(MpiConstants.FIELD_SYSTEM));
		assertEquals(MpiConstants.HOME, resourceTelecoms.get(3).get(FIELD_USE));
		assertEquals(home2, resourceTelecoms.get(3).get(MpiConstants.FIELD_VALUE));
		assertEquals(homeAttributeUuid2, resourceTelecoms.get(3).get(FIELD_ID));
	}
	
	@Test
	public void generateMessageHeader_shouldCreateMessageHeaderForIntegration() {
		Map<String, Object> messageHeader = FhirUtils.generateMessageHeader();
		
		@SuppressWarnings("unchecked")
		Map<String, Object> resourceMap = (Map<String, Object>) messageHeader.get("resource");
		
		assertFalse(messageHeader.isEmpty());
		assertEquals(1, messageHeader.size());
		assertFalse(resourceMap.isEmpty());
		assertEquals(resourceMap.get("eventUri"), MESSAGE_HEADER_EVENT_URI);
		assertEquals(Integer.parseInt((String) resourceMap.get("id")), 1);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> focus = (List<Map<String, Object>>) resourceMap.get("focus");
		assertEquals(focus.get(0).get("reference"), MESSAGE_HEADER_REFERENCE);
		
	}
	
	@Test
	public void fastCreateMap_shouldDoFasterCreateMapWithTwoWithNameAndPhone() {
		Map<String, Object> fasterCreatedMap = FhirUtils.fastCreateMap("name", NAME, "phone", PHONE);
		
		assertTrue(!fasterCreatedMap.isEmpty());
		assertEquals(2, fasterCreatedMap.size());
		assertEquals(fasterCreatedMap.get("name"), NAME);
		assertEquals(fasterCreatedMap.get("phone"), PHONE);
		
	}
	
	@Test
	public void getObjectInMapAsMap_shouldGetObjectInMessageHeaderMap() {
		Map<String, Object> messageHeader = FhirUtils.generateMessageHeader();
		
		Map<String, Object> resource = FhirUtils.getObjectInMapAsMap("resource", messageHeader);
		
		assertFalse(resource.isEmpty());
		assertEquals(resource.get("eventUri"), MESSAGE_HEADER_EVENT_URI);
		assertEquals(Integer.parseInt((String) resource.get("id")), 1);
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> focus = (List<Map<String, Object>>) resource.get("focus");
		assertTrue(!focus.isEmpty());
		assertEquals(focus.get(0).get("reference"), MESSAGE_HEADER_REFERENCE);
	}
	
	@Test
	public void getObjectOnMapAsListOfMap_shouldGetObjectInMapAsListMap() {
		when(MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_EVENT_URI)).thenReturn(MESSAGE_HEADER_EVENT_URI);
		when(MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE)).thenReturn(MESSAGE_HEADER_REFERENCE);
		FhirUtils.initializeCachesIfNecessary();
		Map<String, Object> messageHeader = FhirUtils.generateMessageHeader();
		Map<String, Object> resource = FhirUtils.getObjectInMapAsMap("resource", messageHeader);
		
		assertFalse(resource.isEmpty());
		assertEquals(resource.get("eventUri"), MESSAGE_HEADER_EVENT_URI);
		assertEquals(Integer.parseInt((String) resource.get("id")), 1);
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> focus = FhirUtils.getObjectOnMapAsListOfMap("focus", resource);
		assertTrue(!focus.isEmpty());
		assertEquals(focus.get(0).get("reference"), MESSAGE_HEADER_REFERENCE);
	}
	
	@Test
	public void buildPatient_shouldExcludeHealthFacilityForOpenCR() {
		List<Object> personDetails = asList(null, null, true, null, null, false);
		when(mockMpiContext.getMpiSystem()).thenReturn(MpiSystemType.OPENCR);
		
		Map<String, Object> resource = FhirUtils.buildPatient("1", false, personDetails, null);
		
		assertEquals(1, ((List) resource.get(MpiConstants.FIELD_IDENTIFIER)).size());
		Mockito.verifyZeroInteractions(mockPatientService);
		Mockito.verifyZeroInteractions(mockMpiService);
	}
	
}
