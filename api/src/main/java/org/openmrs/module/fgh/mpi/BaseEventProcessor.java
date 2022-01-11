package org.openmrs.module.fgh.mpi;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for event processors
 */
public abstract class BaseEventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(BaseEventProcessor.class);
	
	private PatientAndPersonEventHandler patientHandler;
	
	private AssociationEventHandler assocHandler;
	
	protected ObjectMapper mapper;
	
	public BaseEventProcessor(PatientAndPersonEventHandler patientHandler, AssociationEventHandler assocHandler) {
		this.patientHandler = patientHandler;
		this.assocHandler = assocHandler;
		mapper = new ObjectMapper();
	}
	
	/**
	 * Creates a fhir patient resource
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
			
			switch (event.getTableName()) {
				case "person":
				case "patient":
					resource = patientHandler.handle(event);
					break;
				case "person_name":
				case "person_address":
				case "patient_identifier":
				case "person_attribute":
					resource = assocHandler.handle(event);
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
