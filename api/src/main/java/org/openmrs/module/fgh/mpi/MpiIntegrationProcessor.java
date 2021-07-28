package org.openmrs.module.fgh.mpi;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.Patient;
import org.openmrs.api.PatientService;
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
	
	@Autowired
	private MpiHttpClient mpiHttpClient;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public void process(String patientUuid) throws Exception {
		log.info("Processing patient with uuid -> " + patientUuid);
		
		PatientService patientService = Context.getPatientService();
		Patient patient = patientService.getPatientByUuid(patientUuid);
		if (patient == null) {
			log.info("No patient found with uuid: " + patientUuid);
			return;
		}
		
		Map<String, Object> fhirResource = new HashMap();
		fhirResource.put("resourceType", "Patient");
		
		mpiHttpClient.postPatient(mapper.writeValueAsString(fhirResource));
	}
	
}
