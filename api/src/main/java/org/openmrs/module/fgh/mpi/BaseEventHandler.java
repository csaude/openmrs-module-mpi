package org.openmrs.module.fgh.mpi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
		final AtomicInteger patientIdRef = new AtomicInteger();
		
		Daemon.runInDaemonThreadAndWait(() -> {
			try {
				log.info("Looking up patient Id associated to the event");
				
				patientIdRef.set(getPatientId(event));
				if (patientIdRef.get() < 1) {
					log.info("No patient id found");
					return;
				}
				
				log.info("Found patient id: " + patientIdRef.get());
				
				processor.process(patientIdRef.get());
			}
			catch (Throwable t) {
				throwableRef.set(t);
			}
			
		}, DaemonTokenHolder.getToken());
		
		if (throwableRef.get() != null) {
			throw new Exception("Error", throwableRef.get());
		}
		
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
