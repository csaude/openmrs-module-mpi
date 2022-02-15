package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiConstants.DATETIME_FORMATTER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CONTACT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_EXTENSION;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_GENDER;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_NAME;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_VALUE_STR;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_ATTRIB_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.HEALTH_CENTER_URL;
import static org.openmrs.module.fgh.mpi.MpiConstants.IDENTIFIER;
import static org.openmrs.module.fgh.mpi.MpiConstants.NAME;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.module.fgh.mpi.MpiUtils.executeQuery;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.PersonAttributeType;
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
	
	protected final static String RELATIONSHIP_QUERY = "SELECT person_a, person_b, relationship, start_date, end_date, uuid FROM relationship "
	        + "WHERE (" + "person_a = " + ID_PLACEHOLDER + " OR person_b = " + ID_PLACEHOLDER
	        + ") AND voided = 0 ORDER BY voided ASC";
	
	public final static String CONTACT_PERSON_QUERY = "SELECT uuid, gender FROM person WHERE person_id = " + ID_PLACEHOLDER;
	
	private final static Map<String, String> ATTR_TYPE_GP_ID_MAP = new HashMap(2);
	
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
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_NAME) != null) {
			existingNameCount = ((List) mpiPatient.get(MpiConstants.FIELD_NAME)).size();
		}
		
		fhirRes.put(MpiConstants.FIELD_NAME, getNames(id, existingNameCount, true));
		
		Integer existingAddressCount = null;
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_ADDRESS) != null) {
			existingAddressCount = ((List) mpiPatient.get(MpiConstants.FIELD_ADDRESS)).size();
		}
		
		fhirRes.put(MpiConstants.FIELD_ADDRESS, getAddresses(id, existingAddressCount, true));
		fhirRes.put(MpiConstants.FIELD_TELECOM, getPhones(id, mpiPatient));
		fhirRes.put(MpiConstants.FIELD_CONTACT, getRelationships(id, mpiPatient));
		List<Map<String, Object>> heathCenter = getHealthCenter(id, mpiPatient);
		if (heathCenter != null) {
			fhirRes.put(MpiConstants.FIELD_EXTENSION, heathCenter);
		}
		
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
		List<List<Object>> idRows = executeQuery(ID_QUERY.replace(ID_PLACEHOLDER, patientId));
		int idListLength = idRows.size() + 1;
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_IDENTIFIER) != null) {
			idListLength = ((List) mpiPatient.get(MpiConstants.FIELD_IDENTIFIER)).size();
		}
		
		List<Map<String, Object>> identifiers = new ArrayList(idListLength);
		Map<String, Object> sourceIdRes = new HashMap();
		sourceIdRes.put(MpiConstants.FIELD_SYSTEM, MpiConstants.SOURCE_ID_URI);
		sourceIdRes.put(MpiConstants.FIELD_VALUE, person.get(4));
		identifiers.add(sourceIdRes);
		
		idRows.stream().forEach(idRow -> {
			Map<String, Object> idResource = new HashMap();
			idResource.put(MpiConstants.FIELD_ID, idRow.get(2));
			String system = MpiConstants.SYSTEM_PREFIX + idRow.get(1);
			idResource.put(MpiConstants.FIELD_SYSTEM, system);
			idResource.put(MpiConstants.FIELD_VALUE, idRow.get(0));
			identifiers.add(idResource);
		});
		
		//We need to overwrite all ids at all indices for an existing patient otherwise OpenCR(hapi fhir) will write 
		//each identifier data by matching indices of our list with the existing list which will be problematic if our
		//list and existing list are of different length
		while (identifiers.size() < idListLength) {
			identifiers.add(null);
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
		int nameListLength = nameRows.size();
		if (existingNameCount != null) {
			nameListLength = existingNameCount;
		}
		
		List<Map<String, Object>> names = new ArrayList(nameListLength);
		boolean foundPreferred = false;
		for (List<Object> nameRow : nameRows) {
			Map<String, Object> nameRes = new HashMap();
			nameRes.put(MpiConstants.FIELD_ID, nameRow.get(4));
			nameRes.put(MpiConstants.FIELD_PREFIX, nameRow.get(0));
			
			List<Object> givenNames = new ArrayList(2);
			givenNames.add(nameRow.get(1));
			givenNames.add(nameRow.get(2));
			
			nameRes.put(MpiConstants.FIELD_GIVEN, givenNames);
			nameRes.put(MpiConstants.FIELD_FAMILY, nameRow.get(3));
			//TODO Add family name suffix and degree as suffix fields
			
			nameRes.put(MpiConstants.FIELD_USE, !foundPreferred ? MpiConstants.USE_OFFICIAL : null);
			if (!foundPreferred) {
				foundPreferred = true;
			}
			
			names.add(nameRes);
		}
		
		while (names.size() < nameListLength) {
			names.add(null);
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
		int addressListLength = addressRows.size();
		if (existingAddressCount != null) {
			addressListLength = existingAddressCount;
		}
		
		List<Map<String, Object>> addresses = new ArrayList(addressListLength);
		for (List<Object> addressRow : addressRows) {
			Map<String, Object> addressResource = new HashMap();
			addressResource.put(MpiConstants.FIELD_ID, addressRow.get(10));
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
			
			period.put(MpiConstants.FIELD_START, startDate);
			period.put(MpiConstants.FIELD_END, endDate);
			addressResource.put(MpiConstants.FIELD_PERIOD, period);
			
			addresses.add(addressResource);
		}
		
		while (addresses.size() < addressListLength) {
			addresses.add(null);
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
		List<List<Object>> phoneRows = getAttributes(personId, MpiConstants.GP_PHONE_MOBILE);
		Map<String, Object> phoneResource = null;
		if (!phoneRows.isEmpty()) {
			if (phoneRows.size() > 1) {
				throw new APIException("Found multiple mobile phone attribute values for the same patient");
			}
			
			phoneResource = new HashMap();
			phoneResource.put(MpiConstants.FIELD_ID, phoneRows.get(0).get(1));
			phoneResource.put(MpiConstants.FIELD_SYSTEM, MpiConstants.PHONE);
			phoneResource.put(MpiConstants.FIELD_VALUE, phoneRows.get(0).get(0));
			phoneResource.put(MpiConstants.FIELD_USE, MpiConstants.MOBILE);
		}
		
		int phoneListLength = 2;
		if (mpiPerson != null && mpiPerson.get(MpiConstants.FIELD_TELECOM) != null) {
			phoneListLength = ((List) mpiPerson.get(MpiConstants.FIELD_TELECOM)).size();
		}
		
		List<Map<String, Object>> phones = new ArrayList(phoneListLength);
		phones.add(phoneResource);
		
		phoneRows = getAttributes(personId, MpiConstants.GP_PHONE_HOME);
		phoneResource = null;
		if (!phoneRows.isEmpty()) {
			if (phoneRows.size() > 1) {
				throw new APIException("Found multiple home phone attribute values for the same person");
			}
			
			phoneResource = new HashMap();
			phoneResource.put(MpiConstants.FIELD_ID, phoneRows.get(0).get(1));
			phoneResource.put(MpiConstants.FIELD_SYSTEM, MpiConstants.PHONE);
			phoneResource.put(MpiConstants.FIELD_VALUE, phoneRows.get(0).get(0));
			phoneResource.put(MpiConstants.FIELD_USE, MpiConstants.HOME);
		}
		
		phones.add(phoneResource);
		
		while (phones.size() < phoneListLength) {
			phones.add(null);
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
		Map<String, Object> healthCenterExt = null;
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
			uuidExt.put(FIELD_VALUE_STR, location.getUuid());
			Map<String, String> nameExt = new HashMap(2);
			nameExt.put(FIELD_URL, NAME);
			nameExt.put(FIELD_VALUE_STR, location.getName());
			healthCenterExt = new HashMap(2);
			healthCenterExt.put(FIELD_URL, HEALTH_CENTER_URL);
			healthCenterExt.put(FIELD_EXTENSION, Arrays.asList(uuidExt, nameExt));
			
			return Collections.singletonList(healthCenterExt);
		} else if (mpiPatient != null && mpiPatient.get(FIELD_EXTENSION) != null) {
			Map<String, String> uuidExt = new HashMap(2);
			uuidExt.put(FIELD_URL, IDENTIFIER);
			uuidExt.put(FIELD_VALUE_STR, null);
			Map<String, String> nameExt = new HashMap(2);
			nameExt.put(FIELD_URL, NAME);
			nameExt.put(FIELD_VALUE_STR, null);
			healthCenterExt = new HashMap(2);
			healthCenterExt.put(FIELD_URL, HEALTH_CENTER_URL);
			healthCenterExt.put(FIELD_EXTENSION, Arrays.asList(uuidExt, nameExt));
			
			return Collections.singletonList(healthCenterExt);
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
			
			String attTypeUuid = Context.getAdministrationService().getGlobalProperty(globalProperty);
			if (StringUtils.isBlank(attTypeUuid)) {
				throw new APIException("No value found for global property named: " + globalProperty);
			}
			
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
		int relationshipLength = relationshipRows.size();
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_CONTACT) != null) {
			relationshipLength = ((List) mpiPatient.get(MpiConstants.FIELD_CONTACT)).size();
		}
		
		List<Map> relationships = new ArrayList(relationshipLength);
		for (List<Object> relationshipRow : relationshipRows) {
			Map<String, Object> resource = new HashMap();
			Integer otherPersonId = (Integer) (patientId.equals(relationshipRow.get(0)) ? relationshipRow.get(1)
			        : relationshipRow.get(0));
			List<List<Object>> otherPersonDetails = executeQuery(
			    CONTACT_PERSON_QUERY.replace(ID_PLACEHOLDER, otherPersonId.toString()));
			resource.put(FIELD_ID, patientId.equals(otherPersonDetails.get(0)));
			
			if (otherPersonDetails.size() == 1) {
				String gender = otherPersonDetails.get(1) != null ? otherPersonDetails.get(1).toString() : null;
				resource.put(FIELD_GENDER, gender);
			}
			
			if (mpiPatient != null && mpiPatient.get(FIELD_CONTACT) != null) {
				List<Map> contacts = (List) mpiPatient.get(FIELD_CONTACT);
				Map existingContact = getExistingContactByUuid(null, contacts);
				Integer existingNameCount = null;
				if (existingContact != null && existingContact.get(MpiConstants.FIELD_NAME) != null) {
					existingNameCount = ((List) existingContact.get(MpiConstants.FIELD_NAME)).size();
				}
				
				resource.put(FIELD_NAME, getNames(otherPersonId.toString(), existingNameCount, false));
				
				Integer existingAddressCount = null;
				if (existingContact != null && existingContact.get(MpiConstants.FIELD_ADDRESS) != null) {
					existingAddressCount = ((List) existingContact.get(MpiConstants.FIELD_ADDRESS)).size();
				}
				
				resource.put(MpiConstants.FIELD_ADDRESS,
				    getAddresses(otherPersonId.toString(), existingAddressCount, false));
				
				resource.put(MpiConstants.FIELD_TELECOM, getPhones(otherPersonId.toString(), existingContact));
				
				Map<String, Object> period = new HashMap();
				String startDate = null;
				if (relationshipRow.get(3) != null && StringUtils.isNotBlank(relationshipRow.get(3).toString())) {
					startDate = DATETIME_FORMATTER.format(Timestamp.valueOf(relationshipRow.get(3).toString()));
				}
				
				String endDate = null;
				if (relationshipRow.get(4) != null && StringUtils.isNotBlank(relationshipRow.get(4).toString())) {
					endDate = DATETIME_FORMATTER.format(Timestamp.valueOf(relationshipRow.get(4).toString()));
				}
				
				period.put(MpiConstants.FIELD_START, startDate);
				period.put(MpiConstants.FIELD_END, endDate);
				resource.put(MpiConstants.FIELD_PERIOD, period);
			}
			
			relationships.add(resource);
		}
		
		while (relationships.size() < relationshipLength) {
			relationships.add(null);
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
	 * Looks the contact from the specified list that matches the specified uuid
	 * 
	 * @param uuid the person uuid to match
	 * @param contacts the list of contacts
	 * @return the matching contact otherwise null
	 */
	private static Map getExistingContactByUuid(String uuid, List<Map> contacts) {
		for (Map contact : contacts) {
			if (contact.get(FIELD_ID) != null && uuid.equalsIgnoreCase(contact.get(FIELD_ID).toString())) {
				return contact;
			}
		}
		
		return null;
	}
	
}
