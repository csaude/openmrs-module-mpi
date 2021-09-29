package org.openmrs.module.fgh.mpi;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utility methods for event processors
 */
public class ProcessorUtils {
	
	private static final Logger log = LoggerFactory.getLogger(ProcessorUtils.class);
	
	/**
	 * Creates a fhir patient resource
	 *
	 * @param event the {@link DatabaseEvent} object to process
	 * @param patientHandler {@link PatientAndPersonEventHandler} instance
	 * @param assocHandler {@link AssociationEventHandler} instance
	 * @throws Throwable
	 */
	public static Map<String, Object> createFhirResource(DatabaseEvent event, PatientAndPersonEventHandler patientHandler,
	        AssociationEventHandler assocHandler) throws Throwable {
		
		Map<String, Object> resource = null;
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("Start: create fhir resource");
			}
			
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
			
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
			}
			finally {
				Context.closeSession();
			}
		}
		
	}
}
