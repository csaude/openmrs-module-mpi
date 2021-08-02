package org.openmrs.module.fgh.mpi;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for patient table events
 */
@Component("patientEventHandler")
public class PatientEventHandler extends BaseEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);
	
	@Override
	public Patient getPatient(DatabaseEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Handling patient event -> " + event);
		}
		
		if (event.getOperation() == DatabaseOperation.CREATE || event.getOperation() == DatabaseOperation.UPDATE
		        || event.getOperation() == DatabaseOperation.READ) {
			
			return Context.getPatientService().getPatient(Integer.valueOf(event.getPrimaryKeyId().toString()));
		}
		
		//TODO mark a delete patient as inactive in the MPI
		log.info("Ignoring" + event.getOperation() + " patient event");
		
		return null;
	}
	
}
