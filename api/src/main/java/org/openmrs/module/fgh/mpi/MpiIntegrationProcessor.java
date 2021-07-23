package org.openmrs.module.fgh.mpi;

import java.util.function.Consumer;

import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("mpiIntegrationProcessor")
public class MpiIntegrationProcessor implements Consumer<DatabaseEvent> {
	
	private static final Logger log = LoggerFactory.getLogger(MpiIntegrationProcessor.class);
	
	@Override
	public void accept(DatabaseEvent event) {
		log.info("Processing database event -> " + event);
	}
	
}
