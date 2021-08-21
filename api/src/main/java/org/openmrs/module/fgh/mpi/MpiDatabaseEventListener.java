package org.openmrs.module.fgh.mpi;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseEventListener;
import org.openmrs.module.debezium.DebeziumConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(DebeziumConstants.DB_EVENT_LISTENER_BEAN_NAME)
public class MpiDatabaseEventListener implements DatabaseEventListener {
	
	private static final Logger log = LoggerFactory.getLogger(MpiDatabaseEventListener.class);
	
	@Autowired
	private PatientAndPersonEventHandler patientHandler;
	
	@Autowired
	private AssociationEventHandler associationHandler;
	
	private boolean snapshotOnly;
	
	private EventProcessor eventProcessor;
	
	@Override
	public void init(boolean snapshotOnly) {
		this.snapshotOnly = snapshotOnly;
		if (snapshotOnly) {
			eventProcessor = new SnapshotEventProcessor(patientHandler);
		} else {
			eventProcessor = new IncrementalEventProcessor(patientHandler, associationHandler);
		}
	}
	
	/**
	 * @see DatabaseEventListener#getTablesToInclude()
	 */
	@Override
	public Set<String> getTablesToInclude() {
		if (snapshotOnly) {
			return Collections.singleton("patient");
		}
		
		return Arrays.stream(MpiConstants.WATCHED_TABLES).collect(Collectors.toSet());
	}
	
	@Override
	public void onEvent(DatabaseEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Received database event -> " + event);
		}
		
		eventProcessor.process(event);
	}
	
}
