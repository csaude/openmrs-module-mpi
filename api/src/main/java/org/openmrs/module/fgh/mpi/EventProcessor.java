package org.openmrs.module.fgh.mpi;

import org.openmrs.module.debezium.DatabaseEvent;

/**
 * Super interface for event processors
 */
public interface EventProcessor {
	
	/**
	 * called to process an event
	 * 
	 * @param event {@link DatabaseEvent} obkect
	 */
	void process(DatabaseEvent event);
	
}
