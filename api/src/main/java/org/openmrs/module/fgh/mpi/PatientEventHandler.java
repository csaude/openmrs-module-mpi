package org.openmrs.module.fgh.mpi;

import org.openmrs.module.debezium.DatabaseEvent;
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
	public Integer getPatientId(DatabaseEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Handling patient event -> " + event);
		}
		
		Integer patientId = Integer.valueOf(event.getPrimaryKeyId().toString());
		if (log.isDebugEnabled()) {
			log.debug("Patient Id: " + patientId);
		}
		
		return patientId;
	}
	
}
