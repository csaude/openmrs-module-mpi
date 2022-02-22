package org.openmrs.module.fgh.mpi;

import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for patient and person table events
 */
@Component("patientAndPersonEventHandler")
public class PatientAndPersonEventHandler extends BaseEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(PatientAndPersonEventHandler.class);
	
	@Override
	public Integer getPatientId(DatabaseEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Handling " + event.getTableName() + " event -> " + event);
		}
		
		return Integer.valueOf(event.getPrimaryKeyId().toString());
	}
	
}
