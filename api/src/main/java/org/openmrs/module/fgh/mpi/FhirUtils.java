package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiConstants.DATETIME_FORMATTER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ADDRESS;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CODE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CODING;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CONTACT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_DISPLAY;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_END;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_EXTENSION;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_GENDER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_NAME;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_PERIOD;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_RELATIONSHIP;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_START;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_TELECOM;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_TEXT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_TYPE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_USE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_VALUE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_VALUE_STR;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_VALUE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_HEALTH_CENTER_EXT_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_IDENTIFIER_TYPE_CONCEPT_MAP;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_IDENTIFIER_TYPE_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_ID_TYPE_SYSTEM_MAP;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_OPENMRS_UUID_CONCEPT_MAP;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_PERSON_UUID_EXT_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_RELATIONSHIP_TYPE_CONCEPT_MAP_A;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_RELATIONSHIP_TYPE_CONCEPT_MAP_B;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_RELATIONSHIP_TYPE_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_EVENT_URI;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_UUID_SYSTEM;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_ATTRIB_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.IDENTIFIER;
import static org.openmrs.module.fgh.mpi.MpiConstants.NAME;
import static org.openmrs.module.fgh.mpi.MpiConstants.UUID_PREFIX;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiUtils.executeQuery;
import static java.util.Collections.singletonList;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.RelationshipType;
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utilities for working with fhir resources
 */
public class FhirUtils {
	
	private static final Logger log = LoggerFactory.getLogger(FhirUtils.class);
	
	public final static String ATTR_TYPE_ID_PLACEHOLDER = "{ATTR_TYPE_ID}";
	
	protected final static String ID_QUERY = "SELECT i.identifier, t.uuid, i.uuid FROM patient_identifier i, "
	        + "patient_identifier_type t WHERE i.identifier_type = t.patient_identifier_type_id AND i.patient_id " + "= "
	        + ID_PLACEHOLDER + " AND i.voided = 0 ORDER BY preferred DESC";
	
	protected final static String NAME_QUERY = "SELECT prefix, given_name, middle_name, family_name, uuid FROM person_name "
	        + "WHERE " + "person_id = " + ID_PLACEHOLDER + " AND voided = 0 ORDER BY preferred DESC";
	
	protected final static String ADDRESS_QUERY = "SELECT address1, address2, address3, address5, address6, "
	        + "county_district, state_province, country, start_date, end_date, uuid FROM person_address WHERE "
	        + "person_id = " + ID_PLACEHOLDER + " AND voided = 0 ORDER BY preferred DESC";
	
	protected final static String ATTR_QUERY = "SELECT value, uuid FROM person_attribute WHERE person_id = " + ID_PLACEHOLDER
	        + " AND person_attribute_type_id = " + ATTR_TYPE_ID_PLACEHOLDER + " AND voided = 0";
	
	protected final static String RELATIONSHIP_QUERY = "SELECT r.person_a, r.person_b, r.start_date, r.end_date, r.uuid, "
	        + "t.uuid FROM relationship r, relationship_type t WHERE r.relationship = t.relationship_type_id AND (r.person_a = "
	        + ID_PLACEHOLDER + " OR r.person_b = " + ID_PLACEHOLDER + ") AND r.voided = 0 ORDER BY r.voided ASC";
	
	public final static String CONTACT_PERSON_QUERY = "SELECT gender, uuid FROM person WHERE person_id = " + ID_PLACEHOLDER;
	
	private final static Map<String, String> ATTR_TYPE_GP_ID_MAP = new HashMap(2);
	
	private static String healthCenterExtUrl;
	
	private static String personUuidExtUrl;
	
	private static String relationshipTypeSystem;
	
	private static String idTypeSystem;
	
	private static String openmrsUuidSystem;
	
	private static Map<String, String> idSystemMap;
	
	private static String openmrsUuidCode;
	
	private static String openmrsUuidDisplay;
	
	private static Map<String, TypeConcept> uuidRelationshipTypePersonAConceptMap;
	
	private static Map<String, TypeConcept> uuidRelationshipTypePersonBConceptMap;
	
	private static Map<String, TypeConcept> uuidIdentifierTypeMap;
	
	public static String santeMessageHeaderFocusReference;
	
	public static String santeMessageHeaderEventUri;
	
	/**
	 * Builds a map of fields and values with patient details that can be serialized as a fhir json
	 * message. This method looks up the up to date patient details from the DB bypassing any hibernate
	 * caches which might be outdated since DB sync is operating outside of the OpenMRS API.
	 * 
	 * @param id the patient id
	 * @param patientVoided specifies if patient is voided or not
	 * @param person a list of column values from the person table
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @return field and value map of the patient details
	 */
	public static Map<String, Object> buildPatient(String id, boolean patientVoided, List<Object> person,
	        Map<String, Object> mpiPatient) {
		
		initializeCachesIfNecessary();
		Map<String, Object> fhirRes = new HashMap();
		fhirRes.put(MpiConstants.FIELD_RESOURCE_TYPE, MpiConstants.PATIENT);
		fhirRes.put(MpiConstants.FIELD_ACTIVE, !patientVoided);
		
		String fhirGender = convertToFhirGender(person.get(0) != null ? person.get(0).toString() : null);
		fhirRes.put(MpiConstants.FIELD_GENDER, fhirGender);
		
		String birthDate = person.get(1) != null ? person.get(1).toString() : "";
		fhirRes.put(MpiConstants.FIELD_BIRTHDATE, birthDate);
		
		String dead = person.get(2).toString();
		if (Boolean.valueOf(dead)) {
			String deathDateStr = person.get(3) != null ? person.get(3).toString() : null;
			if (StringUtils.isBlank(deathDateStr)) {
				fhirRes.put(MpiConstants.FIELD_DECEASED, Boolean.valueOf(dead));
			} else {
				Date deathDate = Timestamp.valueOf(deathDateStr);
				fhirRes.put(MpiConstants.FIELD_DECEASED_DATE, DATETIME_FORMATTER.format(deathDate));
			}
		} else {
			fhirRes.put(MpiConstants.FIELD_DECEASED, Boolean.valueOf(dead));
			fhirRes.put(MpiConstants.FIELD_DECEASED_DATE, null);
		}
		
		fhirRes.put(MpiConstants.FIELD_IDENTIFIER, getIds(id, person, mpiPatient));
		Integer existingNameCount = null;
		if (mpiPatient != null && mpiPatient.get(FIELD_NAME) != null) {
			existingNameCount = ((List) mpiPatient.get(FIELD_NAME)).size();
		}
		
		fhirRes.put(FIELD_NAME, getNames(id, existingNameCount, true));
		
		Integer existingAddressCount = null;
		if (mpiPatient != null && mpiPatient.get(FIELD_ADDRESS) != null) {
			existingAddressCount = ((List) mpiPatient.get(FIELD_ADDRESS)).size();
		}
		
		fhirRes.put(FIELD_ADDRESS, getAddresses(id, existingAddressCount, true));
		fhirRes.put(FIELD_TELECOM, getPhones(id, mpiPatient));
		fhirRes.put(FIELD_CONTACT, getRelationships(id, mpiPatient));
		
		/*List<Map<String, Object>> heathCenter = getHealthCenter(id, mpiPatient);
		if (heathCenter != null) {
			fhirRes.put(MpiConstants.FIELD_EXTENSION, heathCenter);
		}*/
		
		return fhirRes;
	}
	
	/**
	 * Generates and returns the patient identifier list
	 * 
	 * @param patientId id the patient id
	 * @param person a list of column values from the person table
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @return list of the patient identifiers
	 */
	private static List<Map<String, Object>> getIds(String patientId, List<Object> person, Map<String, Object> mpiPatient) {
		Map codingResource = new HashMap();
		codingResource.put(FIELD_SYSTEM, idTypeSystem);
		codingResource.put(FIELD_CODE, openmrsUuidCode);
		codingResource.put(FIELD_DISPLAY, openmrsUuidDisplay);
		Map sourceIdTypeResource = new HashMap();
		sourceIdTypeResource.put(FIELD_CODING, singletonList(codingResource));
		sourceIdTypeResource.put(FIELD_TEXT, MpiConstants.OPENMRS_UUID);
		Map<String, Object> sourceIdRes = new HashMap();
		sourceIdRes.put(FIELD_SYSTEM, openmrsUuidSystem);
		sourceIdRes.put(FIELD_VALUE, person.get(4));
		sourceIdRes.put(FIELD_TYPE, sourceIdTypeResource);
		List<Map<String, Object>> identifiers = new ArrayList();
		identifiers.add(sourceIdRes);
		
		List<List<Object>> idRows = executeQuery(ID_QUERY.replace(ID_PLACEHOLDER, patientId));
		idRows.stream().forEach(idRow -> {
			Map<String, Object> idResource = new HashMap();
			idResource.put(FIELD_ID, idRow.get(2));
			final String identifierTypeUuid = idRow.get(1).toString();
			if (StringUtils.isBlank(idSystemMap.get(identifierTypeUuid))) {
				throw new APIException("No id system uri defined for identifier type with uuid: " + identifierTypeUuid);
			}
			
			idResource.put(FIELD_SYSTEM, idSystemMap.get(identifierTypeUuid));
			idResource.put(FIELD_VALUE, idRow.get(0));
			
			TypeConcept concept = uuidIdentifierTypeMap.get(identifierTypeUuid);
			if (concept == null) {
				throw new APIException("No concept mapped to patient identifier type with uuid: " + identifierTypeUuid);
			}
			
			Map idCodingResource = new HashMap();
			idCodingResource.put(FIELD_SYSTEM, idTypeSystem);
			idCodingResource.put(FIELD_CODE, concept.code);
			idCodingResource.put(FIELD_DISPLAY, concept.display);
			Map idTypeResource = new HashMap();
			idTypeResource.put(FIELD_CODING, singletonList(idCodingResource));
			idTypeResource.put(FIELD_TEXT, concept.text);
			idResource.put(FIELD_TYPE, idTypeResource);
			
			identifiers.add(idResource);
		});
		
		//We need to overwrite all ids at all indices for an existing patient otherwise OpenCR(hapi fhir) will write 
		//each identifier data by matching indices of our list with the existing list which will be problematic if our
		//list and existing list are of different length
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_IDENTIFIER) != null) {
			int mpiIdListLength = ((List) mpiPatient.get(MpiConstants.FIELD_IDENTIFIER)).size();
			while (identifiers.size() < mpiIdListLength) {
				identifiers.add(null);
			}
		}
		
		return identifiers;
	}
	
	/**
	 * Generates and returns the person name list
	 *
	 * @param personId id the person id
	 * @param existingNameCount the count of names of the person record fetched from the MPI
	 * @return list of the person names
	 */
	private static List<Map<String, Object>> getNames(String personId, Integer existingNameCount, boolean getAll) {
		final String query = getAll ? NAME_QUERY : NAME_QUERY + " LIMIT 1";
		List<List<Object>> nameRows = executeQuery(query.replace(ID_PLACEHOLDER, personId));
		List<Map<String, Object>> names = new ArrayList();
		boolean foundPreferred = false;
		for (List<Object> nameRow : nameRows) {
			Map<String, Object> nameRes = new HashMap();
			nameRes.put(FIELD_ID, nameRow.get(4));
			nameRes.put(MpiConstants.FIELD_PREFIX, nameRow.get(0));
			
			List<Object> givenNames = new ArrayList(2);
			givenNames.add(nameRow.get(1));
			givenNames.add(nameRow.get(2));
			
			nameRes.put(MpiConstants.FIELD_GIVEN, givenNames);
			nameRes.put(MpiConstants.FIELD_FAMILY, nameRow.get(3));
			//TODO Add family name suffix and degree as suffix fields
			
			nameRes.put(FIELD_USE, !foundPreferred ? MpiConstants.USE_OFFICIAL : null);
			if (!foundPreferred) {
				foundPreferred = true;
			}
			
			names.add(nameRes);
		}
		
		if (existingNameCount != null) {
			while (names.size() < existingNameCount) {
				names.add(null);
			}
		}
		
		return names;
	}
	
	/**
	 * Generates and returns the patient address list
	 *
	 * @param personId id the person id
	 * @param existingAddressCount the count of addresses of the patient record fetched from the MPI
	 * @return list of the person's addresses
	 */
	private static List<Map<String, Object>> getAddresses(String personId, Integer existingAddressCount, boolean getAll) {
		final String query = getAll ? ADDRESS_QUERY : ADDRESS_QUERY + " LIMIT 1";
		List<List<Object>> addressRows = executeQuery(query.replace(ID_PLACEHOLDER, personId));
		List<Map<String, Object>> addresses = new ArrayList();
		for (List<Object> addressRow : addressRows) {
			Map<String, Object> addressResource = new HashMap();
			addressResource.put(FIELD_ID, addressRow.get(10));
			List<Object> lineRes = new ArrayList(5);
			lineRes.add(addressRow.get(1));//address2
			lineRes.add(addressRow.get(4));//address6
			lineRes.add(addressRow.get(3));//address5
			lineRes.add(addressRow.get(2));//address3
			lineRes.add(addressRow.get(0));//address1
			addressResource.put(MpiConstants.FIELD_LINE, lineRes);
			addressResource.put(MpiConstants.FIELD_DISTRICT, addressRow.get(5));
			addressResource.put(MpiConstants.FIELD_STATE, addressRow.get(6));
			addressResource.put(MpiConstants.FIELD_COUNTRY, addressRow.get(7));
			
			Map<String, Object> period = new HashMap();
			String startDate = null;
			if (addressRow.get(8) != null && StringUtils.isNotBlank(addressRow.get(8).toString())) {
				startDate = DATETIME_FORMATTER.format(Timestamp.valueOf(addressRow.get(8).toString()));
			}
			
			String endDate = null;
			if (addressRow.get(9) != null && StringUtils.isNotBlank(addressRow.get(9).toString())) {
				endDate = DATETIME_FORMATTER.format(Timestamp.valueOf(addressRow.get(9).toString()));
			}
			
			period.put(FIELD_START, startDate);
			period.put(FIELD_END, endDate);
			addressResource.put(FIELD_PERIOD, period);
			
			addresses.add(addressResource);
		}
		
		if (existingAddressCount != null) {
			while (addresses.size() < existingAddressCount) {
				addresses.add(null);
			}
		}
		
		return addresses;
	}
	
	/**
	 * Generates and returns the person phone number list
	 *
	 * @param personId id the person id
	 * @param mpiPerson a map of person fields and values from the MPI
	 * @return list of the person's telephones
	 */
	private static List<Map<String, Object>> getPhones(String personId, Map<String, Object> mpiPerson) {
		List<List<Object>> mobilePhoneRows = getAttributes(personId, MpiConstants.GP_PHONE_MOBILE);
		List<List<Object>> homePhoneRows = getAttributes(personId, MpiConstants.GP_PHONE_HOME);
		List<Map<String, Object>> phones = new ArrayList();
		for (List<Object> phoneRow : mobilePhoneRows) {
			Map<String, Object> phoneResource = new HashMap();
			phoneResource.put(FIELD_ID, phoneRow.get(1));
			phoneResource.put(FIELD_SYSTEM, MpiConstants.PHONE);
			phoneResource.put(FIELD_VALUE, phoneRow.get(0));
			phoneResource.put(FIELD_USE, MpiConstants.MOBILE);
			phones.add(phoneResource);
		}
		
		for (List<Object> phoneRow : homePhoneRows) {
			Map<String, Object> phoneResource = new HashMap();
			phoneResource.put(FIELD_ID, phoneRow.get(1));
			phoneResource.put(FIELD_SYSTEM, MpiConstants.PHONE);
			phoneResource.put(FIELD_VALUE, phoneRow.get(0));
			phoneResource.put(FIELD_USE, MpiConstants.HOME);
			phones.add(phoneResource);
		}
		
		if (mpiPerson != null && mpiPerson.get(FIELD_TELECOM) != null) {
			int mpiPhoneListLength = ((List) mpiPerson.get(FIELD_TELECOM)).size();
			while (phones.size() < mpiPhoneListLength) {
				phones.add(null);
			}
		}
		
		return phones;
	}
	
	/**
	 * Generates and returns the patient health center as the assigning org
	 *
	 * @param patientId patientId the patient id
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @return list of extensions containing only the patient's health center
	 */
	private static List<Map<String, Object>> getHealthCenter(String patientId, Map<String, Object> mpiPatient) {
		String attTypeId = Context.getPersonService().getPersonAttributeTypeByUuid(HEALTH_CENTER_ATTRIB_TYPE_UUID).getId()
		        .toString();
		String phoneQuery = ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, attTypeId);
		List<List<Object>> healthCenterRows = executeQuery(phoneQuery);
		LocationService ls = Context.getLocationService();
		if (!healthCenterRows.isEmpty()) {
			if (healthCenterRows.size() > 1) {
				throw new APIException("Found multiple health center attribute values for the same person");
			}
			
			Object locationId = healthCenterRows.get(0).get(0);
			Location location = ls.getLocation(Integer.valueOf(locationId.toString()));
			if (location == null) {
				throw new APIException("No location found with id: " + locationId);
			}
			
			Map<String, String> uuidExt = new HashMap(2);
			uuidExt.put(FIELD_URL, IDENTIFIER);
			uuidExt.put(FIELD_VALUE_UUID, UUID_PREFIX + location.getUuid());
			Map<String, String> nameExt = new HashMap(2);
			nameExt.put(FIELD_URL, NAME);
			nameExt.put(FIELD_VALUE_STR, location.getName());
			Map<String, Object> healthCenterExt = new HashMap(2);
			healthCenterExt.put(FIELD_URL, healthCenterExtUrl);
			healthCenterExt.put(FIELD_EXTENSION, Arrays.asList(uuidExt, nameExt));
			
			return singletonList(healthCenterExt);
		} else if (mpiPatient != null && mpiPatient.get(FIELD_EXTENSION) != null) {
			Map<String, String> uuidExt = new HashMap(2);
			uuidExt.put(FIELD_URL, IDENTIFIER);
			uuidExt.put(FIELD_VALUE_UUID, null);
			Map<String, String> nameExt = new HashMap(2);
			nameExt.put(FIELD_URL, NAME);
			nameExt.put(FIELD_VALUE_STR, null);
			Map<String, Object> healthCenterExt = new HashMap(2);
			healthCenterExt.put(FIELD_URL, healthCenterExtUrl);
			healthCenterExt.put(FIELD_EXTENSION, Arrays.asList(uuidExt, nameExt));
			
			return singletonList(healthCenterExt);
		}
		
		return null;
	}
	
	/**
	 * Gets person attributes for the patient with the specified id and the attribute type with a uuid
	 * matching the value of the specified global property name.
	 * 
	 * @param patientId the patient id
	 * @param globalProperty the global property name
	 * @return list of person attributes
	 */
	private static List<List<Object>> getAttributes(String patientId, String globalProperty) {
		String attTypeId = ATTR_TYPE_GP_ID_MAP.get(globalProperty);
		if (attTypeId == null) {
			if (log.isDebugEnabled()) {
				log.debug("Loading person attribute type associated to the global property named: " + globalProperty);
			}
			
			String attTypeUuid = MpiUtils.getGlobalPropertyValue(globalProperty);
			PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeTypeByUuid(attTypeUuid);
			if (attributeType == null) {
				throw new APIException("No person attribute type found with uuid: " + attTypeUuid);
			}
			
			attTypeId = attributeType.getId().toString();
			ATTR_TYPE_GP_ID_MAP.put(globalProperty, attTypeId);
		}
		
		return executeQuery(ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, attTypeId));
	}
	
	/**
	 * Generates and returns the patient relationship list i.e contacts in fhir
	 *
	 * @param patientId id the patient id
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @return list of the patient relationships
	 */
	private static List<Map> getRelationships(String patientId, Map<String, Object> mpiPatient) {
		List<List<Object>> relationshipRows = executeQuery(RELATIONSHIP_QUERY.replace(ID_PLACEHOLDER, patientId));
		List<Map> relationships = new ArrayList();
		for (List<Object> relationshipRow : relationshipRows) {
			final String relationshipTypeUuid = relationshipRow.get(5).toString();
			Integer otherPersonId;
			boolean isPersonA = false;
			if (patientId.equals(relationshipRow.get(0).toString())) {
				otherPersonId = (Integer) (relationshipRow.get(1));
			} else {
				isPersonA = true;
				otherPersonId = (Integer) (relationshipRow.get(0));
			}
			
			TypeConcept concept = getRelationshipTypeConcept(relationshipTypeUuid, isPersonA);
			if (concept == null) {
				throw new APIException("No concept mapped to person " + (isPersonA ? "A" : "B")
				        + " of the relationship type with uuid: " + relationshipTypeUuid);
			}
			
			Map codingResource = new HashMap();
			codingResource.put(FIELD_SYSTEM, relationshipTypeSystem);
			codingResource.put(FIELD_CODE, concept.code);
			codingResource.put(FIELD_DISPLAY, concept.display);
			Map relationshipTypeResource = new HashMap();
			relationshipTypeResource.put(FIELD_CODING, singletonList(codingResource));
			relationshipTypeResource.put(FIELD_TEXT, concept.text);
			Map<String, Object> resource = new HashMap();
			resource.put(FIELD_RELATIONSHIP, relationshipTypeResource);
			List<List<Object>> otherPersonDetails = executeQuery(
			    CONTACT_PERSON_QUERY.replace(ID_PLACEHOLDER, otherPersonId.toString()));
			final String relationshipUuid = relationshipRow.get(4).toString();
			resource.put(FIELD_ID, relationshipUuid);
			String gender = otherPersonDetails.get(0).get(0) != null ? otherPersonDetails.get(0).get(0).toString() : null;
			resource.put(FIELD_GENDER, convertToFhirGender(gender));
			Map personUuidExt = new HashMap(2);
			personUuidExt.put(FIELD_URL, personUuidExtUrl);
			personUuidExt.put(FIELD_VALUE_UUID, UUID_PREFIX + otherPersonDetails.get(0).get(1));
			resource.put(FIELD_EXTENSION, singletonList(personUuidExt));
			
			Map existingContact = null;
			if (mpiPatient != null && mpiPatient.get(FIELD_CONTACT) != null) {
				List<Map> contacts = (List) mpiPatient.get(FIELD_CONTACT);
				existingContact = getExistingContactByUuid(relationshipUuid, contacts);
			}
			
			resource.put(FIELD_NAME, getNames(otherPersonId.toString(), 1, false).get(0));
			resource.put(FIELD_ADDRESS, getAddresses(otherPersonId.toString(), 1, false).get(0));
			resource.put(FIELD_TELECOM, getPhones(otherPersonId.toString(), existingContact));
			
			Map<String, Object> period = new HashMap();
			String startDate = null;
			if (relationshipRow.get(2) != null && StringUtils.isNotBlank(relationshipRow.get(2).toString())) {
				startDate = DATETIME_FORMATTER.format(Timestamp.valueOf(relationshipRow.get(2).toString()));
			}
			
			String endDate = null;
			if (relationshipRow.get(3) != null && StringUtils.isNotBlank(relationshipRow.get(3).toString())) {
				endDate = DATETIME_FORMATTER.format(Timestamp.valueOf(relationshipRow.get(3).toString()));
			}
			
			period.put(FIELD_START, startDate);
			period.put(FIELD_END, endDate);
			resource.put(FIELD_PERIOD, period);
			
			relationships.add(resource);
		}
		
		if (mpiPatient != null && mpiPatient.get(FIELD_CONTACT) != null) {
			int mpiRelationshipLength = ((List) mpiPatient.get(FIELD_CONTACT)).size();
			while (relationships.size() < mpiRelationshipLength) {
				relationships.add(null);
			}
		}
		
		return relationships;
	}
	
	/**
	 * Converts a the specified gender value to the fhir equivalent
	 * 
	 * @param openmrsGender the OpenMRS gender value to convert
	 * @return the fhir gender value
	 */
	private static String convertToFhirGender(String openmrsGender) {
		String fhirGender = "";
		if ("M".equalsIgnoreCase(openmrsGender)) {
			fhirGender = MpiConstants.GENDER_MALE;
		} else if ("F".equalsIgnoreCase(openmrsGender)) {
			fhirGender = MpiConstants.GENDER_FEMALE;
		} else if ("O".equalsIgnoreCase(openmrsGender)) {
			fhirGender = MpiConstants.GENDER_OTHER;
		} else if (StringUtils.isBlank(openmrsGender)) {
			fhirGender = MpiConstants.GENDER_UNKNOWN;
		} else if (openmrsGender != null) {
			throw new APIException("Don't know how to represent in fhir gender value: " + openmrsGender);
		}
		
		return fhirGender;
	}
	
	/**
	 * Looks the contact from the specified list that matches the specified relationship uuid
	 * 
	 * @param relationshipUuid the relationship uuid to match
	 * @param contacts the list of contacts
	 * @return the matching contact details otherwise null
	 */
	private static Map getExistingContactByUuid(String relationshipUuid, List<Map> contacts) {
		for (Map contact : contacts) {
			if (contact.get(FIELD_ID) != null && relationshipUuid.equalsIgnoreCase(contact.get(FIELD_ID).toString())) {
				return contact;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the {@link TypeConcept} associated to the OpenMRS RelationshipType with the specified uuid
	 * 
	 * @param relationshipTypeUuid the relationship type uuid to match
	 * @param isPersonA specifies the person's side of the relationship type the concept maps to
	 * @return FhirRelationshipType object
	 */
	private synchronized static TypeConcept getRelationshipTypeConcept(String relationshipTypeUuid, boolean isPersonA) {
		Map<String, TypeConcept> relMap = isPersonA ? uuidRelationshipTypePersonAConceptMap
		        : uuidRelationshipTypePersonBConceptMap;
		
		if (relMap == null) {
			relMap = new HashMap();
			final String gpName = isPersonA ? GP_RELATIONSHIP_TYPE_CONCEPT_MAP_A : GP_RELATIONSHIP_TYPE_CONCEPT_MAP_B;
			String maps = MpiUtils.getGlobalPropertyValue(gpName);
			if (StringUtils.isNotBlank(maps)) {
				for (String map : maps.trim().split(",")) {
					String[] details = map.trim().split(":");
					final String uuid = details[0].trim();
					RelationshipType type = Context.getPersonService().getRelationshipTypeByUuid(uuid);
					if (type == null) {
						throw new APIException("No relationship type found with uuid: " + uuid);
					}
					
					final String text = isPersonA ? type.getaIsToB() : type.getbIsToA();
					relMap.put(uuid, new TypeConcept(details[1].trim(), details[2].trim(), text));
				}
			}
		}
		
		return relMap.get(relationshipTypeUuid);
	}
	
	/**
	 * Loads and caches the necessary global property values
	 */
	protected synchronized static void initializeCachesIfNecessary() {
		if (idTypeSystem == null) {
			idTypeSystem = MpiUtils.getGlobalPropertyValue(GP_IDENTIFIER_TYPE_SYSTEM);
		}
		
		if (openmrsUuidSystem == null) {
			openmrsUuidSystem = MpiUtils.getGlobalPropertyValue(GP_UUID_SYSTEM);
		}
		
		if (healthCenterExtUrl == null) {
			healthCenterExtUrl = MpiUtils.getGlobalPropertyValue(GP_HEALTH_CENTER_EXT_URL);
		}
		
		if (personUuidExtUrl == null) {
			personUuidExtUrl = MpiUtils.getGlobalPropertyValue(GP_PERSON_UUID_EXT_URL);
		}
		
		if (relationshipTypeSystem == null) {
			relationshipTypeSystem = MpiUtils.getGlobalPropertyValue(GP_RELATIONSHIP_TYPE_SYSTEM);
		}
		
		if (openmrsUuidCode == null || openmrsUuidDisplay == null) {
			String openmrsUuidConceptMap = MpiUtils.getGlobalPropertyValue(GP_OPENMRS_UUID_CONCEPT_MAP);
			String[] mapDetails = openmrsUuidConceptMap.trim().split(":");
			openmrsUuidCode = mapDetails[0];
			openmrsUuidDisplay = mapDetails[1];
		}
		
		if (idSystemMap == null) {
			idSystemMap = new HashMap();
			String maps = MpiUtils.getGlobalPropertyValue(GP_ID_TYPE_SYSTEM_MAP);
			if (StringUtils.isNotBlank(maps)) {
				for (String map : maps.trim().split(",")) {
					String[] details = map.trim().split("\\^");
					idSystemMap.put(details[0].trim(), details[1].trim());
				}
			}
		}
		
		if (uuidIdentifierTypeMap == null) {
			uuidIdentifierTypeMap = new HashMap();
			String maps = MpiUtils.getGlobalPropertyValue(GP_IDENTIFIER_TYPE_CONCEPT_MAP);
			if (StringUtils.isNotBlank(maps)) {
				for (String map : maps.trim().split(",")) {
					String[] details = map.trim().split(":");
					final String uuid = details[0].trim();
					PatientIdentifierType type = Context.getPatientService().getPatientIdentifierTypeByUuid(uuid);
					if (type == null) {
						throw new APIException("No patient identifier type found with uuid: " + uuid);
					}
					
					uuidIdentifierTypeMap.put(uuid, new TypeConcept(details[1].trim(), details[2].trim(), type.getName()));
				}
			}
		}
		
		if (santeMessageHeaderEventUri == null) {
			santeMessageHeaderEventUri = MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_EVENT_URI);
		}
		
		if (santeMessageHeaderFocusReference == null) {
			santeMessageHeaderFocusReference = MpiUtils.getGlobalPropertyValue(GP_SANTE_MESSAGE_HEADER_FOCUS_REFERENCE);
		}
	}
	
	/**
	 * Generates a fhir map for Message Header needed by santeMPI when submit a bundle
	 * 
	 * @return a map containing the the message header objects
	 */
	public static Map<String, Object> generateMessageHeader() {
		Map<String, Object> messageHeader = new HashMap<String, Object>();
		
		List<Map<String, Object>> focus = new ArrayList<Map<String, Object>>();
		focus.add(fastCreateMap("reference", santeMessageHeaderFocusReference));
		
		Map<String, Object> resourceMap = fastCreateMap("resourceType", "MessageHeader", "id", "1", "eventUri",
		    santeMessageHeaderEventUri, "focus", focus);
		
		messageHeader.put("resource", resourceMap);
		
		return messageHeader;
	}
	
	/**
	 * Create a map populated with an initial entries passed by parameter
	 * 
	 * @param params the entries which will populate the map. It's an array which emulate a map entries
	 *            in this format [key1, val1, key2, val2, key3, val3, ..]
	 * @return the generated map
	 * @throws APIException when the params array length is not odd
	 */
	public static Map<String, Object> fastCreateMap(Object... params) throws APIException {
		if (params.length % 2 != 0)
			throw new APIException("The parameters for fastCreatMap must be pars <K1, V1>, <K2, V2>");
		
		Map<String, Object> map = new HashMap<>();
		
		int paramsSize = params.length / 2;
		
		for (int set = 1; set <= paramsSize; set++) {
			int pos = set * 2 - 1;
			
			map.put(((String) params[pos - 1]), params[pos]);
		}
		
		return map;
	}
	
	/**
	 * Retrieves an object from a map as a {@link Map}
	 * 
	 * @param key the key of the map object which is being retrieved
	 * @param map the map from where the object will be retrieved from
	 * @return the map object retrieved
	 * @throws ClassCastException if the correspondent object for key is not a map
	 */
	public static Map<String, Object> getObjectInMapAsMap(String key, Map<String, Object> map) throws ClassCastException {
		return (Map<String, Object>) map.get(key);
		
	}
	
	/**
	 * Retrieves an object from a map as a {@link List}
	 * 
	 * @param key the key of the object which is being retrieved
	 * @param map the map from where the object will be retrieved from
	 * @return the list object retrieved
	 * @throws ClassCastException if the correspondent object for key is not a list
	 */
	public static List<Map<String, Object>> getObjectOnMapAsListOfMap(String key, Map<String, Object> map)
	        throws ClassCastException {
		return (List<Map<String, Object>>) map.get(key);
	}
	
	public static KeyStore getKeyStoreInstanceByType(String keyStoreType) throws KeyStoreException {
		return KeyStore.getInstance(keyStoreType);
	}
	
	public static KeyManagerFactory getKeyManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
		return KeyManagerFactory.getInstance(algorithm);
	}
	
	public static SSLContext getSslContextByProtocol(String protocol) throws NoSuchAlgorithmException {
		return SSLContext.getInstance("TLSv1.2");
	}
	
	private static class TypeConcept {
		
		private String code;
		
		private String display;
		
		private String text;
		
		private TypeConcept(String code, String display, String text) {
			this.code = code;
			this.display = display;
			this.text = text;
		}
		
		@Override
		public String toString() {
			return "code=" + code + ", display=" + display + ", text=" + text;
		}
	}
	
}
