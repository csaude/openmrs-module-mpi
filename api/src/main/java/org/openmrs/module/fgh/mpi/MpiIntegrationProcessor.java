package org.openmrs.module.fgh.mpi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An instance of this class takes a patient uuid, loads the patient record, generates the fhir json
 * payload and calls the http client to post the patient to the MPI.
 */
@Component("mpiIntegrationProcessor")
public class MpiIntegrationProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(MpiIntegrationProcessor.class);
	
	public final static String FIELD_RESOURCE_TYPE = "resourceType";
	
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
	
	public final static DateFormat BIRTH_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	
	public final static DateFormat DEATH_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	
	@Autowired
	private MpiHttpClient mpiHttpClient;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public void process(Patient patient) throws Exception {
		log.info("Processing patient -> " + patient);
		
		Map<String, Object> fhirRes = new HashMap();
		fhirRes.put(FIELD_RESOURCE_TYPE, "Patient");
		
		List<Map<String, Object>> identifiers = new ArrayList(patient.getActiveIdentifiers().size() + 1);
		Map<String, Object> identifierRes = new HashMap();
		identifierRes.put(FIELD_SYSTEM, SYSTEM_SOURCE_ID);
		identifierRes.put(FIELD_VALUE, patient.getUuid());
		identifiers.add(identifierRes);
		
		patient.getActiveIdentifiers().forEach(id -> {
			Map<String, Object> idResource = new HashMap();
			idResource.put(FIELD_SYSTEM, SYSTEM_PREFIX + id.getIdentifierType().getUuid());
			idResource.put(FIELD_VALUE, id.getIdentifier());
			identifiers.add(idResource);
		});
		
		fhirRes.put(FIELD_IDENTIFIER, identifiers);
		fhirRes.put(FIELD_ACTIVE, !patient.getVoided());
		
		List<Map<String, Object>> names = new ArrayList(patient.getNames().size());
		patient.getNames().stream().filter(name -> !name.getVoided()).forEach(name -> {
			Map<String, Object> nameRes = new HashMap();
			if (StringUtils.isNotBlank(name.getPrefix())) {
				nameRes.put(FIELD_PREFIX, name.getPrefix());
			}
			nameRes.put(FIELD_FAMILY, name.getFamilyName());
			List<String> givenNames = new ArrayList(2);
			givenNames.add(name.getGivenName());
			if (StringUtils.isNotBlank(name.getMiddleName())) {
				givenNames.add(name.getMiddleName());
			}
			nameRes.put(FIELD_GIVEN, givenNames);
			nameRes.put(FIELD_USE, USE_OFFICIAL);
			names.add(nameRes);
		});
		
		fhirRes.put(FIELD_NAME, names);
		String gender = null;
		if ("M".equalsIgnoreCase(patient.getGender())) {
			gender = "male";
		} else if ("F".equalsIgnoreCase(patient.getGender())) {
			gender = "female";
		} else if ("O".equalsIgnoreCase(patient.getGender())) {
			gender = "other";
		} else if (patient.getGender() != null) {
			throw new APIException("Don't know how to represent in fhir the gender: " + patient.getGender());
		}
		
		if (gender != null) {
			fhirRes.put(FIELD_GENDER, gender);
		} else {
			fhirRes.put(FIELD_GENDER, null);
		}
		
		if (patient.getBirthdate() != null) {
			fhirRes.put(FIELD_BIRTHDATE, BIRTH_DATE_FORMATTER.format(patient.getBirthdate()));
		}
		
		if (patient.getDead()) {
			if (patient.getDeathDate() == null) {
				fhirRes.put(FIELD_DECEASED, patient.getDead());
			} else {
				fhirRes.put(FIELD_DECEASED_DATE, DEATH_DATE_FORMATTER.format(patient.getDeathDate()));
			}
		} else {
			fhirRes.put(FIELD_DECEASED, patient.getDead());
			fhirRes.put(FIELD_DECEASED_DATE, null);
		}
		
		List<Map<String, Object>> addresses = new ArrayList(patient.getAddresses().size() + 1);
		patient.getAddresses().stream().filter(address -> !address.getVoided()).forEach(address -> {
			Map<String, Object> addressResource = new HashMap();
			addressResource.put(FIELD_LINE, address.getAddress1());
			addressResource.put(FIELD_CITY, address.getCityVillage());
			addresses.add(addressResource);
		});
		
		fhirRes.put(FIELD_ADDRESS, addresses);
		
		//TODO Add person attributes, add GPs to map attribute types to FHIR fields
		
		mpiHttpClient.submitPatient(mapper.writeValueAsString(fhirRes));
		
		//TODO Add the MPI id to list of the patient's identifiers
	}
	
}
