package org.openmrs.module.fgh.mpi;

import java.util.concurrent.atomic.AtomicReference;

import org.openmrs.Patient;
import org.openmrs.api.context.Daemon;
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
	
	public void handle(DatabaseEvent event) throws Exception {
		
		final AtomicReference<Throwable> throwableRef = new AtomicReference();
		final AtomicReference<Patient> patientRef = new AtomicReference();
		
		Daemon.runInDaemonThreadAndWait(() -> {
			try {
				log.info("Looking up patient associated to the event");
				
				patientRef.set(getPatient(event));
				if (patientRef.get() == null) {
					log.info("No patient found");
					return;
				}
				
				log.info("Found patient: " + patientRef.get());
				
				processor.process(patientRef.get());
			}
			catch (Throwable t) {
				throwableRef.set(t);
			}
			
		}, DaemonTokenHolder.getToken());
		
		//TODO Skip event if the patient is currently in the ignore list or if it's a person/patient delete
		
		if (throwableRef.get() != null) {
			throw new Exception("Error", throwableRef.get());
		}
		
	}
	
	/**
	 * Retrieve the patient record from the specified event instance
	 * 
	 * @param event DatabaseEvent object
	 * @return the Patient object
	 * @throws Exception
	 */
	public abstract Patient getPatient(DatabaseEvent event) throws Exception;
	
}
