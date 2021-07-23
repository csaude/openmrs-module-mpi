package org.openmrs.module.fgh.mpi;

import java.util.function.Consumer;

import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("mpiEventConsumer")
public class DatabaseEventConsumer implements Consumer<DatabaseEvent> {
	
	private static final Logger log = LoggerFactory.getLogger(DatabaseEventConsumer.class);
	
	@Autowired
	@Qualifier("mpiIntegrationProcessor")
	private Consumer<DatabaseEvent> processor;
	
	@Override
	public void accept(DatabaseEvent event) {
		log.info("Received database event -> " + event);
		processor.accept(event);
	}
	
}
