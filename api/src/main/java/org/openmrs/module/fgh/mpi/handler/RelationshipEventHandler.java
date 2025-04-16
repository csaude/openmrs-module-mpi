package org.openmrs.module.fgh.mpi.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.fgh.mpi.utils.MpiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for events in the relationship table
 */
@Component("relationshipEventHandler")
public class RelationshipEventHandler extends BaseEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(RelationshipEventHandler.class);
	
	private static final String COLUMN_PERSON_A = "person_a";
	
	private static final String COLUMN_PERSON_B = "person_b";
	
	public Map<String, Object> handle(DatabaseEvent event) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Handling " + event.getTableName() + " event -> " + event);
		}
		
		log.info("Looking up the ids of the persons associated to the relationship event");
		
		//There can be 2-4 affected patient records for an update event
		Set<Integer> personIds = new HashSet();
		if (event.getPreviousState() != null) {
			personIds.add(Integer.valueOf(event.getPreviousState().get(COLUMN_PERSON_A).toString()));
			personIds.add(Integer.valueOf(event.getPreviousState().get(COLUMN_PERSON_B).toString()));
		}
		
		if (event.getNewState() != null) {
			personIds.add(Integer.valueOf(event.getNewState().get(COLUMN_PERSON_A).toString()));
			personIds.add(Integer.valueOf(event.getNewState().get(COLUMN_PERSON_B).toString()));
		}
		
		log.info("Affected person ids: " + personIds);
		
		List<Map<String, Object>> fhirPatients = new ArrayList(personIds.size());
		for (Integer personId : personIds) {
			Map<String, Object> resource = processor.process(personId, event);
			if (resource != null) {
				fhirPatients.add(resource);
			}
		}
		
		Map<String, Object> patientBundle = new HashMap(2);
		patientBundle.put(MpiConstants.FIELD_RESOURCE_TYPE, MpiConstants.BUNDLE);
		patientBundle.put(MpiConstants.FIELD_ENTRY, fhirPatients);
		
		return patientBundle;
	}
	
	/**
	 * @see BaseEventHandler#getPatientId(DatabaseEvent)
	 */
	@Override
	public Integer getPatientId(DatabaseEvent event) {
		//This event is usually linked to multiple people so we can't return a single patientId
		return null;
	}
	
}
