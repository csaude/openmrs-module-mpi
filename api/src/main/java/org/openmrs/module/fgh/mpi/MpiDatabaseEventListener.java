package org.openmrs.module.fgh.mpi;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DatabaseEventListener;
import org.openmrs.module.debezium.DebeziumConstants;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component(DebeziumConstants.DB_EVENT_LISTENER_BEAN_NAME)
public class MpiDatabaseEventListener implements DatabaseEventListener {
	
	private static final Logger log = LoggerFactory.getLogger(MpiDatabaseEventListener.class);
	
	@Autowired
	@Qualifier("patientAndPersonEventHandler")
	private BaseEventHandler patientHandler;
	
	@Autowired
	@Qualifier("associationEventHandler")
	private BaseEventHandler associationHandler;
	
	//TOD make this configurable
	private ExecutorService executor = Executors.newFixedThreadPool(MpiConstants.DEFAULT_THREAD_COUNT);
	
	@Override
	public void onEvent(DatabaseEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Received database event -> " + event);
		}
		
		final AtomicReference<Throwable> throwableRef = new AtomicReference();
		
		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			Thread.currentThread().setName(event.getTableName() + "-" + event.getPrimaryKeyId());
			log.info("Processing database event -> " + event);
			
			try {
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
				
				log.info("Done processing database event -> " + event);
			}
			catch (Throwable e) {
				throwableRef.set(e);
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
			
		}, executor);
		
		CompletableFuture<Void> compositeFuture = CompletableFuture.allOf(future);
		try {
			if (log.isDebugEnabled()) {
				log.debug("Waiting for processing of event(s) to complete");
			}
			
			compositeFuture.get();
			
			if (log.isDebugEnabled()) {
				log.debug("Future(s) completed");
			}
		}
		catch (Exception e) {
			log.error("Error while waiting for processing of event(s) to complete", e);
		}
		
		if (throwableRef.get() != null) {
			throw new APIException(throwableRef.get());
		}
		
	}
	
	/**
	 * @see DatabaseEventListener#getTablesToInclude(boolean)
	 */
	@Override
	public Set<String> getTablesToInclude(boolean snapshotOnly) {
		if (snapshotOnly) {
			return Collections.singleton("patient");
		}
		
		return Arrays.stream(MpiConstants.WATCHED_TABLES).collect(Collectors.toSet());
	}
	
}
