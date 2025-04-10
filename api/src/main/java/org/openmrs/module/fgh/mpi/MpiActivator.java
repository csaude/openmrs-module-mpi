/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fgh.mpi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.fgh.mpi.task.MpiIntegrationTask;
import org.openmrs.module.fgh.mpi.utils.MpiUtils;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

import static org.openmrs.util.OpenmrsUtil.getApplicationDataDirectory;

public class MpiActivator extends BaseModuleActivator {
	
	private static final Logger log = LoggerFactory.getLogger(MpiActivator.class);
	
	protected static final String MPI_APPENDER_NAME = "MPI_APPENDER";
	
	protected static final String DIR_MPI = "mpi";
	
	protected static final String DIR_LOGS = "logs";
	
	protected static final String LOG_FILE = "mpi.log";
	
	protected static final String LAYOUT = "%-5p %t - %C{1}.%M(%L) |%d{ISO8601}| %m%n";
	
	protected static final String LOG_FILE_PATTERN = LOG_FILE + ".%d{yyyy-MM-dd}-%i";
	
	protected static final String TASK_NAME = "INCREMENTAL LOAD TASK";
	
	/**
	 * @see BaseModuleActivator#started()
	 */
	@Override
	public void started() {
		log.info("MPI module started");
		log.info("Adding MPI log file to log4j configuration");
		
		try {
			File mpiLogFile = MpiUtils.createPath(getApplicationDataDirectory(), DIR_MPI, DIR_LOGS, LOG_FILE).toFile();
			String logFileName = mpiLogFile.getAbsolutePath();
			String logFilePattern = MpiUtils.createPath(mpiLogFile.getParent(), LOG_FILE_PATTERN).toFile().getAbsolutePath();
			LoggerContext context = (LoggerContext) LogManager.getContext(false);
			Configuration cfg = context.getConfiguration();
			PatternLayout layout = PatternLayout.newBuilder().withPattern(LAYOUT).build();
			TriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder().build();
			//TODO Make max file size configurable
			TriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy("50 MB");
			TriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(timePolicy, sizePolicy);
			RollingFileAppender mpiAppender = RollingFileAppender.newBuilder().setConfiguration(cfg)
			        .setName(MPI_APPENDER_NAME).withFileName(logFileName).setLayout(layout).withAppend(true)
			        .withFilePattern(logFilePattern).withPolicy(policy).build();
			AppenderRef appenderRef = AppenderRef.createAppenderRef(MPI_APPENDER_NAME, null, null);
			LoggerConfig loggerCfg = LoggerConfig.newBuilder().withConfig(cfg).withLoggerName(getMpiLoggerName())
			        .withAdditivity(false).withLevel(Level.INFO).withRefs(new AppenderRef[] { appenderRef }).build();
			loggerCfg.addAppender(mpiAppender, null, null);
			cfg.addLogger(getMpiLoggerName(), loggerCfg);
			mpiAppender.start();
			cfg.addAppender(mpiAppender);
			context.updateLoggers();
			
			this.registerTask();
		}
		catch (Exception e) {
			throw new APIException(e);
		}
		
	}
	
	/**
	 * @see BaseModuleActivator#stopped()
	 */
	@Override
	public void stopped() {
		log.info("MPI module stopped");
		log.info("Removing MPI log file from log4j configuration");
		
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration cfg = context.getConfiguration();
		cfg.removeLogger(getMpiLoggerName());
		cfg.getAppender(MPI_APPENDER_NAME).stop();
		cfg.getAppenders().remove(MPI_APPENDER_NAME);
		context.updateLoggers();
	}
	
	protected String getMpiLoggerName() {
		return getClass().getPackage().getName();
	}
	
	private void registerTask() {
		
		try {
			SchedulerService schedulerService = Context.getService(SchedulerService.class);
			TaskDefinition taskDefinition = new TaskDefinition();
			taskDefinition.setName(TASK_NAME);
			taskDefinition.setTaskClass(MpiIntegrationTask.class.getName());
			taskDefinition.setStartTime(new Date());
			taskDefinition.setRepeatInterval(10L);
			taskDefinition.setStartOnStartup(true);
			taskDefinition.setDescription("A task to call debezium event queue service");
			
			// Check if exists a task running in order to avoid duplicates
			TaskDefinition existingTask = schedulerService.getTaskByName(taskDefinition.getName());
			if (existingTask == null) {
				schedulerService.saveTaskDefinition(taskDefinition);
				schedulerService.scheduleTask(taskDefinition);
				log.info("Scheduled task registered and started: {}", taskDefinition.getName());
			} else {
				log.info("Task already registered: {}", taskDefinition.getName());
			}
		}
		catch (SchedulerException e) {
			log.error("Failed to register scheduled task", e);
		}
		
	}
	
}
