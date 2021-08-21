package org.openmrs.module.fgh.mpi;

import org.openmrs.api.APIException;
import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes incremental events, one at a time
 */
public class IncrementalEventProcessor implements EventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(IncrementalEventProcessor.class);
	
	private PatientAndPersonEventHandler patientHandler;
	
	private AssociationEventHandler associationHandler;
	
	public IncrementalEventProcessor(PatientAndPersonEventHandler patientHandler,
	    AssociationEventHandler associationHandler) {
		
		this.patientHandler = patientHandler;
		this.associationHandler = associationHandler;
	}
	
	@Override
	public void process(DatabaseEvent event) {
		
		try {
			ProcessorUtils.processEvent(event, patientHandler, associationHandler);
		}
		catch (Throwable t) {
			throw new APIException(t);
		}
		
	}
	
}
