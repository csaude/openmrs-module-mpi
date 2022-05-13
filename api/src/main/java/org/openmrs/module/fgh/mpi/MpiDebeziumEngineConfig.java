package org.openmrs.module.fgh.mpi;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DebeziumConstants;
import org.openmrs.module.debezium.DebeziumEngineConfig;
import org.openmrs.module.debezium.SnapshotMode;
import org.openmrs.module.debezium.mysql.MySqlSnapshotMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component(DebeziumConstants.ENGINE_CONFIG_BEAN_NAME)
public class MpiDebeziumEngineConfig implements DebeziumEngineConfig {
	
	private static final Logger log = LoggerFactory.getLogger(MpiDebeziumEngineConfig.class);
	
	private BaseEventProcessor eventProcessor;
	
	/**
	 * @see DebeziumEngineConfig#init()
	 */
	@Override
	public void init() {
		if (getSnapshotMode() == MySqlSnapshotMode.INITIAL) {
			String num = Context.getAdministrationService().getGlobalProperty(MpiConstants.GP_INITIAL_BATCH_SIZE);
			int threadCount;
			if (StringUtils.isNotBlank(num)) {
				threadCount = Integer.valueOf(num);
			} else {
				threadCount = 10;
			}
			
			eventProcessor = new SnapshotEventProcessor(threadCount);
		} else {
			eventProcessor = new IncrementalEventProcessor();
		}
	}
	
	/**
	 * @see DebeziumEngineConfig#getSnapshotMode()
	 */
	@Override
	public SnapshotMode getSnapshotMode() {
		String initial = Context.getAdministrationService().getGlobalProperty(MpiConstants.GP_INITIAL);
		return Boolean.valueOf(initial) ? MySqlSnapshotMode.INITIAL : MySqlSnapshotMode.SCHEMA_ONLY;
	}
	
	/**
	 * @see DebeziumEngineConfig#getTablesToInclude()
	 */
	@Override
	public Set<String> getTablesToInclude() {
		if (getSnapshotMode() == MySqlSnapshotMode.INITIAL) {
			return Collections.singleton("patient");
		}
		
		return Arrays.stream(MpiConstants.WATCHED_TABLES).collect(Collectors.toSet());
	}
	
	/**
	 * @see DebeziumEngineConfig#getEventListener()
	 */
	@Override
	public Consumer<DatabaseEvent> getEventListener() {
		return event -> {
			if (log.isDebugEnabled()) {
				log.debug("Received database event -> " + event);
			}
			
			eventProcessor.process(event);
		};
	}
	
}
