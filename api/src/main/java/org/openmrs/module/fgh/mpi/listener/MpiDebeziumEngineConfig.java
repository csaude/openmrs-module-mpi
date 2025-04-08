/*
package org.openmrs.module.fgh.mpi.listener;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.entity.DebeziumEngineConfig;
import org.openmrs.module.debezium.mysql.MySqlSnapshotMode;
import org.openmrs.module.debezium.mysql.SnapshotMode;
import org.openmrs.module.debezium.utils.DebeziumConstants;
import org.openmrs.module.fgh.mpi.processor.BaseEventProcessor;
import org.openmrs.module.fgh.mpi.processor.IncrementalEventProcessor;
import org.openmrs.module.fgh.mpi.processor.SnapshotEventProcessor;
import org.openmrs.module.fgh.mpi.utils.MpiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component(DebeziumConstants.ENGINE_CONFIG_BEAN_NAME)
public class MpiDebeziumEngineConfig implements DebeziumEngineConfig {

	private static final Logger log = LoggerFactory.getLogger(MpiDebeziumEngineConfig.class);

    */
/**
 * @see DebeziumEngineConfig#init()
 *//*
   
   @Override
   public void init() {
    BaseEventProcessor eventProcessor;
   
    if (getSnapshotMode() == MySqlSnapshotMode.INITIAL) {
   	String num = Context.getAdministrationService().getGlobalProperty(MpiConstants.GP_INITIAL_BATCH_SIZE);
   	int threadCount;
   	if (StringUtils.isNotBlank(num)) {
   		threadCount = Integer.valueOf(num);
   	} else {
   		threadCount = Runtime.getRuntime().availableProcessors();
   	}
   
   	eventProcessor = new SnapshotEventProcessor(threadCount);
   } else {
   	eventProcessor = new IncrementalEventProcessor();
   }
   }
   
   */
/**
 * @see DebeziumEngineConfig#getSnapshotMode()
 *//*
   
   @Override
   public SnapshotMode getSnapshotMode() {
   String initial = Context.getAdministrationService().getGlobalProperty(MpiConstants.GP_INITIAL);
   return Boolean.valueOf(initial) ? MySqlSnapshotMode.INITIAL : MySqlSnapshotMode.SCHEMA_ONLY;
   }
   
   
   
   }
   */
