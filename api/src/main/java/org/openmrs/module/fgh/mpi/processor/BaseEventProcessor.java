package org.openmrs.module.fgh.mpi.processor;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.entity.DatabaseEvent;
import org.openmrs.module.fgh.mpi.handler.AssociationEventHandler;
import org.openmrs.module.fgh.mpi.handler.PatientAndPersonEventHandler;
import org.openmrs.module.fgh.mpi.handler.RelationshipEventHandler;
import org.openmrs.module.fgh.mpi.integ.MpiHttpClient;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for event processors
 */
public abstract class BaseEventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(BaseEventProcessor.class);
	
	protected PatientAndPersonEventHandler patientHandler;
	
	protected AssociationEventHandler assocHandler;
	
	protected RelationshipEventHandler relationshipHandler;
	
	protected MpiHttpClient mpiHttpClient;
	
	protected ObjectMapper mapper;
	
	public BaseEventProcessor(boolean snapshotOnly) {
		this.patientHandler = Context.getRegisteredComponents(PatientAndPersonEventHandler.class).get(0);
		this.mpiHttpClient = Context.getRegisteredComponents(MpiHttpClient.class).get(0);
		mapper = new ObjectMapper();
		
		if (!snapshotOnly) {
			this.assocHandler = Context.getRegisteredComponents(AssociationEventHandler.class).get(0);
			this.relationshipHandler = Context.getRegisteredComponents(RelationshipEventHandler.class).get(0);
		}
	}
	
	/**
	 * Creates a fhir patient resource or bundle of 2 patient resources in case of a relationship table
	 * event, this is because a relationship row references 2 persons which could both be patients.
	 *
	 * @param event the {@link DatabaseEvent} object to process
	 * @throws Throwable
	 */
	public Map<String, Object> createFhirResource(DatabaseEvent event) throws Throwable {
		
		Map<String, Object> resource = null;
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("Start: create fhir resource");
			}
			
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_ENCOUNTER_TYPES);
			
			switch (event.getTableName()) {
				case "person":
				case "patient":
					resource = patientHandler.handle(event);
					break;
				case "person_name":
				case "person_address":
				case "patient_identifier":
				case "person_attribute":
				case "encounter":
					resource = assocHandler.handle(event);
					break;
				case "relationship":
					resource = relationshipHandler.handle(event);
					break;
			}
			
			if (log.isDebugEnabled()) {
				log.debug("End: create fhir resource");
			}
			
			return resource;
		}
		finally {
			try {
				Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
				Context.removeProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
				Context.removeProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
				Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
				Context.removeProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
				Context.removeProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
				Context.removeProxyPrivilege(PrivilegeConstants.GET_ENCOUNTER_TYPES);
			}
			finally {
				Context.closeSession();
			}
		}
		
	}
	
	/**
	 * Called to process an event
	 * 
	 * @param event {@link DatabaseEvent} object
	 */
	public abstract void process(DatabaseEvent event);
	
}
