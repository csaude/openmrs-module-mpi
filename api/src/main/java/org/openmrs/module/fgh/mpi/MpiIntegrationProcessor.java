package org.openmrs.module.fgh.mpi;

import java.util.List;
import java.util.Map;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
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
	
	public final static String ID_PLACEHOLDER = "{PATIENT_ID}";
	
	public final static String PATIENT_QUERY = "SELECT voided FROM patient WHERE patient_id = " + ID_PLACEHOLDER;
	
	public final static String PERSON_QUERY = "SELECT gender, birthdate, dead, death_date, uuid FROM person WHERE "
	        + "person_id = " + ID_PLACEHOLDER;
	
	@Autowired
	private MpiHttpClient mpiHttpClient;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public void process(Integer patientId) throws Exception {
		log.info("Processing patient with id -> " + patientId);
		
		String id = patientId.toString();
		AdministrationService adminService = Context.getAdministrationService();
		List<List<Object>> patient = adminService.executeSQL(PATIENT_QUERY.replace(ID_PLACEHOLDER, id), true);
		if (patient.isEmpty()) {
			log.info("No patient found with id: " + patientId);
			return;
		}
		
		List<List<Object>> person = adminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, id), true);
		if (person.isEmpty()) {
			log.info("No person found with id: " + patientId);
			return;
		}
		
		Map<String, Object> resource = MpiUtils.buildPatientResource(id, patient, person);
		mpiHttpClient.submitPatient(mapper.writeValueAsString(resource));
		
		//TODO Add the MPI id to list of the patient's identifiers
	}
	
}
