package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.debezium.DatabaseOperation.CREATE;
import static org.openmrs.module.debezium.DatabaseOperation.DELETE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ACTIVE;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_CONTACT;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.FIELD_RELATIONSHIP;
import static java.lang.Boolean.valueOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An instance of this class takes a patient uuid, loads the patient record, generates the fhir json
 * payload and calls the http client to post the patient to the MPI.
 */
@Component("mpiIntegrationProcessor")
public class MpiIntegrationProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(MpiIntegrationProcessor.class);
	
	public final static String ID_PLACEHOLDER = "{PATIENT_ID}";
	
	public final static String PATIENT_QUERY = "SELECT voided FROM patient WHERE patient_id = " + ID_PLACEHOLDER;
	
	public final static String PERSON_QUERY = "SELECT gender, birthdate, dead, death_date, uuid, voided FROM person WHERE "
	        + "person_id = " + ID_PLACEHOLDER;
	
	@Autowired
	private MpiHttpClient mpiHttpClient;
	
	private final static ObjectMapper MAPPER = new ObjectMapper();
	
	/**
	 * Process the patient with the specified patient id in the MPI to generate the patient fhir
	 * resource
	 * 
	 * @param patientId the patient id
	 * @param e DatabaseEvent object
	 * @return a map representation of the generated patient fhir resource
	 * @throws Exception
	 */
	public Map<String, Object> process(Integer patientId, DatabaseEvent e) throws Exception {
		log.info("Processing patient with id: " + patientId);
		
		if ("person".equalsIgnoreCase(e.getTableName()) && e.getOperation() == CREATE) {
			log.info("Ignoring person insert event");
			return null;
		}
		
		String id = patientId.toString();
		boolean isPersonDeletedEvent = "person".equalsIgnoreCase(e.getTableName()) && e.getOperation() == DELETE;
		List<List<Object>> person = null;
		String patientUud;
		if (isPersonDeletedEvent) {
			patientUud = e.getPreviousState().get("uuid").toString();
		} else {
			person = MpiUtils.executeQuery(PERSON_QUERY.replace(ID_PLACEHOLDER, id));
			if (person.isEmpty()) {
				log.info("Ignoring event because no person was found with id: " + id);
				return null;
			} else {
				patientUud = person.get(0).get(4).toString();
			}
		}
		
		Map<String, Object> mpiPatient = mpiHttpClient.getPatient(patientUud);
		if (mpiPatient != null) {
			log.info("Found existing patient record in the MPI");
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No patient record found in the MPI");
			}
		}
		
		boolean isMpiPatientActive = mpiPatient == null ? false : valueOf(mpiPatient.get(FIELD_ACTIVE).toString());
		boolean isPatientDeletedEvent = "patient".equalsIgnoreCase(e.getTableName()) && e.getOperation() == DELETE;
		
		if ((mpiPatient == null || !isMpiPatientActive) && (isPatientDeletedEvent || isPersonDeletedEvent)) {
			if (mpiPatient == null) {
				log.info("Ignoring event because there is no record in the MPI to update for deleted "
				        + (isPatientDeletedEvent ? "patient" : "person"));
			} else {
				log.info("Ignoring event because the record in the MPI is already marked as inactive for deleted "
				        + (isPatientDeletedEvent ? "patient" : "person"));
			}
			
			return null;
		}
		
		if (isPatientDeletedEvent || isPersonDeletedEvent) {
			Map<String, Object> fhirResource = mpiPatient;
			fhirResource.put(FIELD_ACTIVE, false);
			return fhirResource;
		} else {
			List<List<Object>> patient = MpiUtils.executeQuery(PATIENT_QUERY.replace(ID_PLACEHOLDER, id));
			if (patient.isEmpty()) {
				if (mpiPatient == null || !isMpiPatientActive) {
					log.info("Ignoring event because there is no patient record both in OpenMRS and MPI");
					if (mpiPatient == null) {
						log.info("Ignoring event because there is no record in the MPI to update");
					} else {
						log.info("Ignoring event because the record in the MPI is already marked as inactive");
					}
					
					return null;
				}
				
				Map<String, Object> fhirResource = mpiPatient;
				fhirResource.put(FIELD_ACTIVE, false);
				return fhirResource;
			} else {
				List<Object> patientDetails = patient.get(0);
				List<Object> personDetails = person.get(0);
				boolean patientVoided = Boolean.valueOf(patientDetails.get(0).toString());
				
				if (mpiPatient == null) {
					if (patientVoided || Boolean.valueOf(personDetails.get(5).toString())) {
						
						//This should effectively skip placeholder patient and person rows
						log.info("Not submitting the patient to the MPI because the person or patient is voided");
						
						return null;
					}
				}
				
				//TODO May be we should not build a new resource and instead update the mpiPatient if one exists
				//And we will need to be aware of placeholder rows
				Map<String, Object> generated = FhirUtils.buildPatient(id, patientVoided, personDetails, mpiPatient);
				
				if (generated.get("name") == null) {
					log.warn("Skipping patient with no name");
					
					return null;
				}
				
				//There is a bug in the MPI where contact.relationship field is never updated for existing contacts,
				//Clear it in the MPI for existing contacts and we will later update it when we resubmit the patient
				if (generated != null && mpiPatient != null) {
					//TODO Avoid overwriting data submitted by third party systems
					if (generated.get(FIELD_CONTACT) != null && mpiPatient.get(FIELD_CONTACT) != null) {
						List mpiContactsToUpdate = new ArrayList();
						for (Map contact : (List<Map>) generated.get(FIELD_CONTACT)) {
							if (contact == null) {
								continue;
							}
							
							for (Map mpiContact : (List<Map>) mpiPatient.get(FIELD_CONTACT)) {
								Object i = mpiContact.get(FIELD_ID);
								if (i != null && contact.get(FIELD_ID).toString().equalsIgnoreCase(i.toString())) {
									mpiContact.put(FIELD_RELATIONSHIP, null);
									mpiContactsToUpdate.add(mpiContact);
								}
							}
						}
						
						if (!mpiContactsToUpdate.isEmpty()) {
							log.info("Clearing relationship types for existing relationships to be updated in the MPI");
							
							mpiHttpClient.submitPatient(MAPPER.writeValueAsString(mpiPatient));
						}
					}
				}
				
				return generated;
			}
		}
	}
	
}
