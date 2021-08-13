package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

/**
 * Contains utility methods
 */
public class MpiUtils {
	
	public final static String FIELD_RESOURCE_TYPE = "resourceType";
	
	public final static String FIELD_ID = "id";
	
	public final static String FIELD_IDENTIFIER = "identifier";
	
	public final static String FIELD_ACTIVE = "active";
	
	public final static String FIELD_NAME = "name";
	
	public final static String FIELD_GENDER = "gender";
	
	public final static String FIELD_BIRTHDATE = "birthDate";
	
	public final static String FIELD_DECEASED = "deceasedBoolean";
	
	public final static String FIELD_DECEASED_DATE = "deceasedDateTime";
	
	public final static String FIELD_SYSTEM = "system";
	
	public final static String FIELD_VALUE = "value";
	
	public final static String FIELD_PREFIX = "prefix";
	
	public final static String FIELD_FAMILY = "family";
	
	public final static String FIELD_GIVEN = "given";
	
	public final static String FIELD_ADDRESS = "address";
	
	public final static String FIELD_USE = "use";
	
	public final static String FIELD_LINE = "line";
	
	public final static String FIELD_DISTRICT = "district";
	
	public final static String FIELD_STATE = "state";
	
	public final static String FIELD_COUNTRY = "country";
	
	public final static String FIELD_PERIOD = "period";
	
	public final static String FIELD_START = "start";
	
	public final static String FIELD_END = "end";
	
	public final static String FIELD_TELECOM = "telecom";
	
	public final static String HOME = "home";
	
	public final static String MOBILE = "mobile";
	
	public final static String USE_OFFICIAL = "official";
	
	public final static String PHONE = "phone";
	
	public final static String SYSTEM_SOURCE_ID = "http://openclientregistry.org/fhir/sourceid";
	
	public final static String SYSTEM_PREFIX = "urn:uuid:";
	
	public final static DateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	
	public final static DateFormat MYSQL_DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public final static String ATTR_TYPE_ID_PLACEHOLDER = "{ATTR_TYPE_ID}";
	
	public final static String ID_QUERY = "SELECT i.identifier, t.uuid, i.uuid FROM patient_identifier i, "
	        + "patient_identifier_type t WHERE i.identifier_type = t.patient_identifier_type_id AND i.patient_id " + "= "
	        + ID_PLACEHOLDER + " AND i.voided = 0 ORDER BY preferred DESC";
	
	public final static String NAME_QUERY = "SELECT prefix, given_name, middle_name, family_name, uuid FROM person_name "
	        + "WHERE " + "person_id = " + ID_PLACEHOLDER + " AND voided = 0 ORDER BY preferred DESC";
	
	public final static String ADDRESS_QUERY = "SELECT address1, address2, address3, address5, address6, "
	        + "county_district, state_province, country, start_date, end_date, uuid FROM person_address WHERE "
	        + "person_id = " + ID_PLACEHOLDER + " AND voided = 0 ORDER BY preferred DESC";
	
	public final static String ATTRIBUTE_QUERY = "SELECT value, uuid FROM person_attribute WHERE person_id = "
	        + ID_PLACEHOLDER + " AND person_attribute_type_id = " + ATTR_TYPE_ID_PLACEHOLDER + " AND voided = 0";
	
	/**
	 * Builds a map of fields and values with patient details that can be serialized as a fhir json
	 * message. This method looks up the up to date patient details from the DB bypassing any hibernate
	 * caches which might be outdated since DB sync is operating outside of the OpenMRS API.
	 * 
	 * @param id the patient id
	 * @param patient a list of column values from the patient table
	 * @param person a list of column values from the person table
	 * @return field and value map of the patient details
	 * @throws Exception
	 */
	public static Map<String, Object> buildPatientResource(String id, List<List<Object>> patient, List<List<Object>> person,
	                                                       Map<String, Object> mpiPatient)
	    throws Exception {
		
		Map<String, Object> fhirRes = new HashMap();
		fhirRes.put(FIELD_RESOURCE_TYPE, "Patient");
		
		fhirRes.put(FIELD_ACTIVE, !Boolean.valueOf(patient.get(0).get(0).toString()));
		
		AdministrationService adminService = Context.getAdministrationService();
		String fhirGender = null;
		String gender = person.get(0).get(0) != null ? person.get(0).get(0).toString() : null;
		if ("M".equalsIgnoreCase(gender)) {
			fhirGender = "male";
		} else if ("F".equalsIgnoreCase(gender)) {
			fhirGender = "female";
		} else if ("O".equalsIgnoreCase(gender)) {
			fhirGender = "other";
		} else if (StringUtils.isBlank(gender)) {
			fhirGender = "unknown";
		} else if (gender != null) {
			throw new APIException("Don't know how to represent in fhir gender value: " + gender);
		}
		
		fhirRes.put(FIELD_GENDER, fhirGender);
		
		String birthDate = person.get(0).get(1) != null ? person.get(0).get(1).toString() : null;
		fhirRes.put(FIELD_BIRTHDATE, birthDate);
		
		String dead = person.get(0).get(2).toString();
		if (Boolean.valueOf(dead)) {
			String deathDateStr = person.get(0).get(3) != null ? person.get(0).get(3).toString() : null;
			if (StringUtils.isBlank(deathDateStr)) {
				fhirRes.put(FIELD_DECEASED, dead);
			} else {
				Date deathDate = MYSQL_DATETIME_FORMATTER.parse(deathDateStr);
				fhirRes.put(FIELD_DECEASED_DATE, DATETIME_FORMATTER.format(deathDate));
			}
		} else {
			fhirRes.put(FIELD_DECEASED, Boolean.valueOf(dead));
			fhirRes.put(FIELD_DECEASED_DATE, null);
		}
		
		List<List<Object>> idRows = adminService.executeSQL(ID_QUERY.replace(ID_PLACEHOLDER, id), true);
		List<Map<String, Object>> identifiers = new ArrayList(idRows.size() + 1);
		Map<String, Object> sourceIdRes = new HashMap();
		sourceIdRes.put(FIELD_SYSTEM, SYSTEM_SOURCE_ID);
		sourceIdRes.put(FIELD_VALUE, person.get(0).get(4));
		identifiers.add(sourceIdRes);
		
		idRows.stream().forEach(idRow -> {
			Map<String, Object> idResource = new HashMap();
			idResource.put(FIELD_ID, idRow.get(2));
			idResource.put(FIELD_SYSTEM, SYSTEM_PREFIX + idRow.get(1));
			idResource.put(FIELD_VALUE, idRow.get(0));
			identifiers.add(idResource);
		});
		
		fhirRes.put(FIELD_IDENTIFIER, identifiers);
		
		List<List<Object>> nameRows = adminService.executeSQL(NAME_QUERY.replace(ID_PLACEHOLDER, id), true);
		List<Map<String, Object>> names = new ArrayList(nameRows.size());
		
		boolean foundPreferred = false;
		for (List<Object> nameRow : nameRows) {
			Map<String, Object> nameRes = new HashMap();
			nameRes.put(FIELD_ID, nameRow.get(4));
			nameRes.put(FIELD_PREFIX, nameRow.get(0));
			
			List<Object> givenNames = new ArrayList(2);
			givenNames.add(nameRow.get(1));
			givenNames.add(nameRow.get(2));
			
			nameRes.put(FIELD_GIVEN, givenNames);
			nameRes.put(FIELD_FAMILY, nameRow.get(3));
			//TODO Add family name suffix and degree as suffix fields
			
			nameRes.put(FIELD_USE, !foundPreferred ? USE_OFFICIAL : null);
			if (!foundPreferred) {
				foundPreferred = true;
			}
			
			names.add(nameRes);
		}
		
		fhirRes.put(FIELD_NAME, names);
		
		List<List<Object>> addressRows = adminService.executeSQL(ADDRESS_QUERY.replace(ID_PLACEHOLDER, id), true);
		List<Map<String, Object>> addresses = new ArrayList(addressRows.size());
		for (List<Object> addressRow : addressRows) {
			Map<String, Object> addressResource = new HashMap();
			addressResource.put(FIELD_ID, addressRow.get(10));
			List<Object> lineRes = new ArrayList(5);
			lineRes.add(addressRow.get(1));//address2
			lineRes.add(addressRow.get(4));//address6
			lineRes.add(addressRow.get(3));//address5
			lineRes.add(addressRow.get(2));//address3
			lineRes.add(addressRow.get(0));//address1
			addressResource.put(FIELD_LINE, lineRes);
			addressResource.put(FIELD_DISTRICT, addressRow.get(5));
			addressResource.put(FIELD_STATE, addressRow.get(6));
			addressResource.put(FIELD_COUNTRY, addressRow.get(7));
			
			Map<String, Object> period = new HashMap();
			String startDate = null;
			if (addressRow.get(8) != null && StringUtils.isNotBlank(addressRow.get(8).toString())) {
				startDate = DATETIME_FORMATTER.format(MYSQL_DATETIME_FORMATTER.parse(addressRow.get(8).toString()));
			}
			
			String endDate = null;
			if (addressRow.get(9) != null && StringUtils.isNotBlank(addressRow.get(9).toString())) {
				endDate = DATETIME_FORMATTER.format(MYSQL_DATETIME_FORMATTER.parse(addressRow.get(9).toString()));
			}
			
			period.put(FIELD_START, startDate);
			period.put(FIELD_END, endDate);
			addressResource.put(FIELD_PERIOD, period);
			
			addresses.add(addressResource);
		}
		
		fhirRes.put(FIELD_ADDRESS, addresses);
		
		PersonService personService = Context.getPersonService();
		String attTypeUuid = adminService.getGlobalProperty(MpiConstants.GP_PHONE_MOBILE);
		String attTypeId = personService.getPersonAttributeTypeByUuid(attTypeUuid).getId().toString();
		String phoneQuery = ATTRIBUTE_QUERY.replace(ID_PLACEHOLDER, id).replace(ATTR_TYPE_ID_PLACEHOLDER, attTypeId);
		List<List<Object>> phoneRows = adminService.executeSQL(phoneQuery, true);
		Map<String, Object> phoneResource = null;
		if (!phoneRows.isEmpty()) {
			phoneResource = new HashMap();
			phoneResource.put(FIELD_ID, phoneRows.get(0).get(1));
			phoneResource.put(FIELD_SYSTEM, PHONE);
			phoneResource.put(FIELD_VALUE, phoneRows.get(0).get(0));
			phoneResource.put(FIELD_USE, MOBILE);
		}
		
		List<Map<String, Object>> phones = new ArrayList(2);
		phones.add(phoneResource);
		
		attTypeUuid = adminService.getGlobalProperty(MpiConstants.GP_PHONE_HOME);
		attTypeId = personService.getPersonAttributeTypeByUuid(attTypeUuid).getId().toString();
		phoneQuery = ATTRIBUTE_QUERY.replace(ID_PLACEHOLDER, id).replace(ATTR_TYPE_ID_PLACEHOLDER, attTypeId);
		phoneRows = adminService.executeSQL(phoneQuery, true);
		phoneResource = null;
		if (!phoneRows.isEmpty()) {
			phoneResource = new HashMap();
			phoneResource.put(FIELD_ID, phoneRows.get(0).get(1));
			phoneResource.put(FIELD_SYSTEM, PHONE);
			phoneResource.put(FIELD_VALUE, phoneRows.get(0).get(0));
			phoneResource.put(FIELD_USE, HOME);
		}
		
		phones.add(phoneResource);
		fhirRes.put(FIELD_TELECOM, phones);
		
		return fhirRes;
	}
	
}
