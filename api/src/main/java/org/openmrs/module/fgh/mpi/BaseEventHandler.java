package org.openmrs.module.fgh.mpi;

import java.util.Map;

import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Base class for all event handlers
 */
public abstract class BaseEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(BaseEventHandler.class);
	
	@Autowired
	@Qualifier("mpiIntegrationProcessor")
	protected MpiIntegrationProcessor processor;
	
	public Map<String, Object> handle(DatabaseEvent event) throws Exception {
		log.info("Looking up patient Id associated to the event");
		
		Integer patientId = getPatientId(event);
		if (patientId == null) {
			log.info("No patient id found");
			return null;
		}
		
		log.info("Found patient id: " + patientId);
		
		return processor.process(patientId, event);
	}
	
	/**
	 * Retrieve the patient id from the specified event instance
	 * 
	 * @param event DatabaseEvent object
	 * @return the Patient Id
	 * @throws Exception
	 */
	public abstract Integer getPatientId(DatabaseEvent event);
	
}
