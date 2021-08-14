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
		List<List<Object>> patientDetails = adminService.executeSQL(PATIENT_QUERY.replace(ID_PLACEHOLDER, id), true);
		if (patientDetails.isEmpty()) {
			log.info("No patient found with id: " + patientId);
			return;
		}
		
		List<List<Object>> person = adminService.executeSQL(PERSON_QUERY.replace(ID_PLACEHOLDER, id), true);
		if (person.isEmpty()) {
			log.info("No person found with id: " + patientId);
			return;
		}
		
		Map<String, Object> mpiPatient = MpiUtils.getPatientFromMpi(person.get(4).toString());
		String mpiId = null;
		if (mpiPatient != null) {
			mpiId = mpiPatient.get("id").toString();
		}
		
		Map<String, Object> resource = MpiUtils.buildPatientResource(id, patientDetails, person, mpiPatient);
		List<Map<String, Map<String, String>>> mpiIdsResponse = mpiHttpClient
		        .submitPatient(mapper.writeValueAsString(resource));
		
		//For a newly registered patient in the MPI, add the MPI ids to list of the patient's identifiers
		if (mpiPatient == null) {
			String newMpiId = extractId(mpiIdsResponse.get(0));
			log.info("Determining the MPI id of the newly created patient MPI record");
			
			Map<String, Object> newMpiPatient = mpiHttpClient.getPatient(newMpiId);
			//Golden record has no details like identifier on it, so if this is one, fetch the other actual record
			if (newMpiPatient.get("identifier") == null) {
				log.info("Found golden record for MPI id " + newMpiId + ", looking up the actual MPI record");
				
				newMpiId = extractId(mpiIdsResponse.get(1));
				newMpiPatient = mpiHttpClient.getPatient(newMpiId);
			}
			
			log.info("New MPI patient record id: " + newMpiId);
		}
	}
	
	/**
	 * Extracts the mpi id from the specified id response payload
	 *
	 * @param mpiIdResponse
	 * @return an array of the ids, the mpi id at index 0 and the global id at index 1
	 */
	private String extractId(Map<String, Map<String, String>> mpiIdResponse) {
		return mpiIdResponse.get("response").get("location").split("/")[1];
	}
	
}
