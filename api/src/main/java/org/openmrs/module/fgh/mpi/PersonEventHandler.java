package org.openmrs.module.fgh.mpi;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for person table events
 */
@Component("personEventHandler")
public class PersonEventHandler extends BaseEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(PersonEventHandler.class);
	
	@Override
	public Patient getPatient(DatabaseEvent event) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Handling person event -> " + event);
		}
		
		if (event.getOperation() == DatabaseOperation.UPDATE || event.getOperation() == DatabaseOperation.READ) {
			Integer personId = Integer.valueOf(event.getPrimaryKeyId().toString());
			Patient patient = Context.getPatientService().getPatient(personId);
			if (patient != null) {
				return patient;
			}
			
			if ((log.isDebugEnabled())) {
				log.debug("Ignoring " + event.getOperation() + " event for person with no patient record");
			}
		} else {
			if ((log.isDebugEnabled())) {
				log.debug("Ignoring " + event.getOperation() + " person event");
			}
		}
		
		return null;
		
	}
	
}
