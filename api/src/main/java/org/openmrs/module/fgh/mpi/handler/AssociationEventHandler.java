package org.openmrs.module.fgh.mpi.handler;

import org.openmrs.module.debezium.entity.DatabaseEvent;
import org.openmrs.module.debezium.entity.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for table events of patient association entities i.e. patient_identifier, person_name and
 * person_address, encounter
 */
@Component("associationEventHandler")
public class AssociationEventHandler extends BaseEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(AssociationEventHandler.class);
	
	@Override
	public Integer getPatientId(DatabaseEvent event) {
		final String tableName = event.getTableName();
		if (log.isDebugEnabled()) {
			log.debug("Handling " + tableName + " event -> " + event);
		}
		
		String columnName = "person_id";
		if ("patient_identifier".equalsIgnoreCase(tableName) || "encounter".equalsIgnoreCase(tableName)) {
			columnName = "patient_id";
		}
		
		Object patientId;
		if (DatabaseOperation.DELETE == event.getOperation()) {
			patientId = event.getPreviousState().get(columnName);
		} else {
			patientId = event.getNewState().get(columnName);
		}
		
		//TODO If person attribute and it's not one of the phone number attribute types ignore
		
		return Integer.valueOf(patientId.toString());
	}
}
