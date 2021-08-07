package org.openmrs.module.fgh.mpi;

import org.openmrs.module.debezium.DatabaseEvent;
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
	public Integer getPatientId(DatabaseEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Handling person event -> " + event);
		}
		
		Integer personId = Integer.valueOf(event.getPrimaryKeyId().toString());
		if (log.isDebugEnabled()) {
			log.debug("Person Id: " + personId);
		}
		
		return personId;
		
	}
	
}
