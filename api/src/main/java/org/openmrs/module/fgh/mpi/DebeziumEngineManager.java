package org.openmrs.module.fgh.mpi;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.module.debezium.DebeziumChangeConsumer;
import org.openmrs.module.debezium.OpenmrsDebeziumEngine;
import org.openmrs.module.debezium.mysql.MySqlDebeziumConfig;
import org.openmrs.module.debezium.mysql.MySqlSnapshotLockMode;
import org.openmrs.module.debezium.mysql.MySqlSnapshotMode;
import org.openmrs.module.debezium.mysql.MySqlSslMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebeziumEngineManager {
	
	private static final Logger log = LoggerFactory.getLogger(DebeziumEngineManager.class);
	
	private static OpenmrsDebeziumEngine engine;
	
	/**
	 * Starts the OpenMRS debezium engine, this method will stop the engine if it's already running
	 * before restarting it.
	 */
	protected synchronized static void start() {
		
		synchronized (DebeziumEngineManager.class) {
			stop();
			
			log.info("Starting OpenMRS debezium engine after context refresh");
			
			AdministrationService adminService = Context.getAdministrationService();
			//TODO support postgres
			//TODO Add a GP to allow admins to enable/disable the debezium engine
			MySqlDebeziumConfig config = new MySqlDebeziumConfig();
			
			String userGp = adminService.getGlobalProperty(MpiConstants.GP_USER);
			if (StringUtils.isNotBlank(userGp)) {
				config.setUsername(userGp);
			} else {
				config.setUsername(Context.getRuntimeProperties().getProperty(MpiConstants.PROP_DB_USERNAME));
			}
			
			String passGp = adminService.getGlobalProperty(MpiConstants.GP_PASSWORD);
			if (StringUtils.isNotBlank(passGp)) {
				config.setPassword(passGp);
			} else {
				config.setPassword(Context.getRuntimeProperties().getProperty(MpiConstants.PROP_DB_PASSWORD));
			}
			
			String includeGp = adminService.getGlobalProperty(MpiConstants.GP_TABLES_TO_INCLUDE);
			if (StringUtils.isNotBlank(includeGp)) {
				config.setTablesToInclude(Arrays.stream(includeGp.split(",")).collect(toSet()));
			}
			String excludeGp = adminService.getGlobalProperty(MpiConstants.GP_TABLES_TO_EXCLUDE);
			if (StringUtils.isNotBlank(excludeGp)) {
				config.setTablesToExclude(Arrays.stream(excludeGp.split(",")).collect(toSet()));
			}
			
			String snapshotModeGp = adminService.getGlobalProperty(MpiConstants.GP_SNAPSHOT_MODE);
			if (StringUtils.isNotBlank(snapshotModeGp)) {
				config.setSnapshotMode(MySqlSnapshotMode.valueOf(snapshotModeGp));
			}
			
			String sslModeGp = adminService.getGlobalProperty(MpiConstants.GP_SSL_MODE);
			if (StringUtils.isNotBlank(sslModeGp)) {
				config.setSslMode(MySqlSslMode.valueOf(sslModeGp));
			}
			
			String snapshotLockGp = adminService.getGlobalProperty(MpiConstants.GP_SNAPSHOT_LOCK_MODE);
			if (StringUtils.isNotBlank(snapshotLockGp)) {
				config.setSnapshotLockMode(MySqlSnapshotLockMode.valueOf(snapshotLockGp));
			}
			
			String jdbcUrl = Context.getRuntimeProperties().getProperty(MpiConstants.PROP_DB_URL);
			String host, portStr, dbName, hostPortDbNameStr;
			if (jdbcUrl.indexOf("?") > -1) {
				hostPortDbNameStr = StringUtils.substringBetween(jdbcUrl, "//", "?");
			} else {
				hostPortDbNameStr = StringUtils.substringAfter(jdbcUrl, "//");
			}
			
			String[] hostPortDbName = StringUtils.split(hostPortDbNameStr, "/");
			String[] hostAndPort = StringUtils.split(hostPortDbName[0], ":");
			host = hostAndPort[0];
			portStr = hostAndPort[1];
			dbName = hostPortDbName[1];
			
			if (log.isDebugEnabled()) {
				log.debug("Connection details used by debezium -> host=" + host + ", port=" + portStr + ", DB=" + dbName);
			}
			
			config.setHost(host);
			config.setPort(Integer.valueOf(portStr));
			config.setDatabaseName(dbName);
			config.setHistoryFilename(adminService.getGlobalProperty(MpiConstants.GP_HISTORY_FILE));
			config.setOffsetStorageFilename(adminService.getGlobalProperty(MpiConstants.GP_OFFSET_STORAGE_FILE));
			
			Consumer<DatabaseEvent> consumer = Context.getRegisteredComponent("mpiEventConsumer",
			    DatabaseEventConsumer.class);
			engine = OpenmrsDebeziumEngine.getInstance();
			config.setConsumer(new DebeziumChangeConsumer(consumer, engine));
			
			engine.start(config);
		}
		
	}
	
	/**
	 * Stops the OpenMRS debezium engine
	 */
	protected synchronized static void stop() {
		
		synchronized (DebeziumEngineManager.class) {
			if (engine != null) {
				log.info("Received call to stop OpenMRS debezium engine");
				
				engine.stop();
				engine = null;
			} else {
				log.info("No running OpenMRS debezium engine found");
			}
		}
		
	}
	
}
