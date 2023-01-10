package org.openmrs.module.fgh.mpi;

import static java.lang.System.currentTimeMillis;

import java.util.List;
import java.util.Map;

import org.openmrs.module.debezium.DatabaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes incremental events, one at a time
 */
public class IncrementalEventProcessor extends BaseEventProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(IncrementalEventProcessor.class);
	
	public IncrementalEventProcessor() {
		super(false);
	}
	
	@Override
	public void process(DatabaseEvent event) {
		
		try {
			log.info("Processing database event -> " + event);
			
			final long start = System.currentTimeMillis();
			
			Map<String, Object> fhirResource = createFhirResource(event);
			
			if (fhirResource != null) {
				//Because a relationship references 2 persons, process all
				if (MpiConstants.BUNDLE.equals(fhirResource.get((MpiConstants.FIELD_RESOURCE_TYPE)))) {
					for (Map<String, Object> fhirPatient : (List<Map>) fhirResource.get(MpiConstants.FIELD_ENTRY)) {
						mpiHttpClient.submitPatient(mapper.writeValueAsString(fhirPatient));
					}
				} else {
					mpiHttpClient.submitPatient(mapper.writeValueAsString(fhirResource));
				}
			}
			
			log.info("Done processing database event -> " + event);
			
			if (log.isDebugEnabled()) {
				log.debug("Duration: " + (currentTimeMillis() - start) + "ms");
			}
		}
		catch (Throwable t) {
			log.error("An error occurred while processing event -> " + event, t);
			//throw new APIException(t);
		}
		
	}
	
}
