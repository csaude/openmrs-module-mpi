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
	
	public final static String FIELD_CITY = "city";
	
	public final static String USE_OFFICIAL = "official";
	
	public final static String SYSTEM_SOURCE_ID = "http://openclientregistry.org/fhir/sourceid";
	
	public final static String SYSTEM_PREFIX = "urn:uuid:";
	
	public final static DateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	
	public final static DateFormat MYSQL_DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public final static String ID_QUERY = "SELECT i.identifier, t.uuid, i.uuid FROM patient_identifier i, "
	        + "patient_identifier_type t WHERE i.identifier_type = t.patient_identifier_type_id AND i.patient_id " + "= "
	        + ID_PLACEHOLDER + " AND i.voided = 0";
	
	public final static String NAME_QUERY = "SELECT prefix, given_name, middle_name, family_name, uuid FROM person_name WHERE "
	        + "person_id = " + ID_PLACEHOLDER + " AND voided = 0";
	
	public final static String ADDRESS_QUERY = "SELECT address1, city_village, uuid FROM person_address WHERE person_id = "
	        + ID_PLACEHOLDER + " AND voided = 0";
	
	public static Map<String, Object> buildPatientResource(String id, List<List<Object>> patient, List<List<Object>> person)
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
		
		nameRows.stream().forEach(nameRow -> {
			Map<String, Object> nameRes = new HashMap();
			nameRes.put(FIELD_ID, nameRow.get(4));
			nameRes.put(FIELD_PREFIX, nameRow.get(0));
			
			List<Object> givenNames = new ArrayList(2);
			givenNames.add(nameRow.get(1));
			givenNames.add(nameRow.get(2));
			
			nameRes.put(FIELD_GIVEN, givenNames);
			nameRes.put(FIELD_FAMILY, nameRow.get(3));
			
			nameRes.put(FIELD_USE, USE_OFFICIAL);
			
			names.add(nameRes);
		});
		
		fhirRes.put(FIELD_NAME, names);
		
		List<List<Object>> addressRows = adminService.executeSQL(ADDRESS_QUERY.replace(ID_PLACEHOLDER, id), true);
		List<Map<String, Object>> addresses = new ArrayList(addressRows.size());
		addressRows.stream().forEach(addressRow -> {
			Map<String, Object> addressResource = new HashMap();
			addressResource.put(FIELD_ID, addressRow.get(2));
			addressResource.put(FIELD_LINE, addressRow.get(0));
			addressResource.put(FIELD_CITY, addressRow.get(1));
			
			addresses.add(addressResource);
		});
		
		fhirRes.put(FIELD_ADDRESS, addresses);
		
		//TODO Add person attributes, add GPs to map attribute types to FHIR fields
		
		return fhirRes;
	}
	
}
