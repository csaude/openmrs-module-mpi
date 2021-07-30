package org.openmrs.module.fgh.mpi;

import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Handler for patient table events
 */
@Component("patientEventHandler")
public class PatientEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);
	
	@Autowired
	@Qualifier("mpiIntegrationProcessor")
	private MpiIntegrationProcessor processor;
	
	public void handle(DatabaseEvent event) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Handling patient event -> " + event);
		}
		
		if (event.getOperation() == DatabaseOperation.CREATE || event.getOperation() == DatabaseOperation.UPDATE
		        || event.getOperation() == DatabaseOperation.READ) {
			
			processor.process(Integer.valueOf(event.getPrimaryKeyId().toString()));
		} else {
			log.debug("Ignoring event for deleted patient -> " + event);
		}
		
	}
	
}
