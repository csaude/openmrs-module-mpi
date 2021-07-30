package org.openmrs.module.fgh.mpi;

import java.util.function.Consumer;

import org.openmrs.api.APIException;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DebeziumConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component(DebeziumConstants.EB_EVENT_CONSUMER_BEAN_NAME)
public class DatabaseEventConsumer implements Consumer<DatabaseEvent> {
	
	private static final Logger log = LoggerFactory.getLogger(DatabaseEventConsumer.class);
	
	@Autowired
	@Qualifier("patientEventHandler")
	private BaseEventHandler patientHandler;
	
	@Autowired
	@Qualifier("personEventHandler")
	private BaseEventHandler personHandler;
	
	@Override
	public void accept(DatabaseEvent event) {
		log.info("Received database event -> " + event);
		
		try {
			switch (event.getTableName()) {
				case "person":
					personHandler.handle(event);
				case "patient":
					patientHandler.handle(event);
			}
		}
		catch (Exception e) {
			throw new APIException("An error occurred while processing database event: " + event, e);
		}
		
	}
	
}
