package org.openmrs.module.fgh.mpi;

import org.openmrs.module.debezium.DatabaseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for event processors
 */
public abstract class BaseEventProcessor {
	
	protected PatientAndPersonEventHandler patientHandler;
	
	protected MpiHttpClient mpiHttpClient;
	
	protected ObjectMapper mapper;
	
	public BaseEventProcessor(PatientAndPersonEventHandler patientHandler, MpiHttpClient mpiHttpClient) {
		this.patientHandler = patientHandler;
		this.mpiHttpClient = mpiHttpClient;
		mapper = new ObjectMapper();
	}
	
	/**
	 * Called to process an event
	 * 
	 * @param event {@link DatabaseEvent} object
	 */
	public abstract void process(DatabaseEvent event);
	
}
