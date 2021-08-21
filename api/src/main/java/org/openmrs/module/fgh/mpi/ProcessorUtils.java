package org.openmrs.module.fgh.mpi;

import static java.lang.System.currentTimeMillis;

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
	 * Processes the specified event
	 * 
	 * @param event the {@link DatabaseEvent} object to process
	 * @param patientHandler {@link PatientAndPersonEventHandler} instance
	 * @param associationHandler {@link AssociationEventHandler} instance
	 * @throws Throwable
	 */
	public static void processEvent(DatabaseEvent event, PatientAndPersonEventHandler patientHandler,
	                                AssociationEventHandler associationHandler)
	    throws Throwable {
		
		try {
			final Long start = System.currentTimeMillis();
			log.info("Processing database event -> " + event);
			
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			
			switch (event.getTableName()) {
				case "person":
				case "patient":
					patientHandler.handle(event);
					break;
				case "person_name":
				case "person_address":
				case "patient_identifier":
				case "person_attribute":
					associationHandler.handle(event);
					break;
			}
			
			log.info("Successfully processed database event -> " + event);
			
			if (log.isDebugEnabled()) {
				log.debug("Duration: " + (currentTimeMillis() - start) + "ms");
			}
		}
		catch (Throwable e) {
			log.error("An error occurred while processing event -> " + event, e);
			throw e;
		}
		finally {
			try {
				Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
				Context.removeProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
			}
			finally {
				Context.closeSession();
			}
		}
		
	}
}
