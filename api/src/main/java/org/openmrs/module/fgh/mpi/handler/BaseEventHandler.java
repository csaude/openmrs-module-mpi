package org.openmrs.module.fgh.mpi.handler;

import java.util.Map;

import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.fgh.mpi.processor.MpiIntegrationProcessor;
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
		log.info("Getting the id of the patient associated to the change event");
		
		Integer patientId = getPatientId(event);
		
		if (log.isDebugEnabled()) {
			log.debug("Patient id: " + patientId);
		}
		
		return processor.process(patientId, event);
	}
	
	/**
	 * Retrieve the patient id from the specified event instance
	 * 
	 * @param event DatabaseEvent object
	 * @return the patient id
	 * @throws Exception
	 */
	public abstract Integer getPatientId(DatabaseEvent event);
	
}
