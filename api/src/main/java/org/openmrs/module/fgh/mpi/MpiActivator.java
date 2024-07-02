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

import static org.openmrs.util.OpenmrsUtil.getApplicationDataDirectory;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.openmrs.api.APIException;
import org.openmrs.module.BaseModuleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiActivator extends BaseModuleActivator {
	
	private static final Logger log = LoggerFactory.getLogger(MpiActivator.class);
	
	protected static final String MPI_APPENDER_NAME = "MPI_APPENDER";
	
	protected static final String DIR_MPI = "mpi";
	
	protected static final String DIR_LOGS = "logs";
	
	protected static final String LOG_FILE = "mpi.log";
	
	protected static final String LAYOUT = "%-5p %t - %C{1}.%M(%L) |%d{ISO8601}| %m%n";
	
	protected static final String LOG_FILE_PATTERN = "mpi-%d{yyyy-MM-dd}.log";
	
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
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			Configuration config = ctx.getConfiguration();
			PatternLayout layout = PatternLayout.newBuilder().withPattern(LAYOUT).build();
			TriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder().build();
			TriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy("10MB");
			TriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(timePolicy, sizePolicy);
			RollingFileAppender mpiAppender = RollingFileAppender.newBuilder().setConfiguration(config)
			        .setName(MPI_APPENDER_NAME).withFileName(logFileName).setLayout(layout).withAppend(true)
			        .withFilePattern(LOG_FILE_PATTERN).withPolicy(policy).build();
			final String name = getClass().getPackage().getName();
			LoggerConfig cfg = LoggerConfig.newBuilder().withConfig(config).withLoggerName(name).withAdditivity(true)
			        .withLevel(Level.INFO).build();
			config.addLogger(MPI_APPENDER_NAME, cfg);
			mpiAppender.start();
			config.addAppender(mpiAppender);
			ctx.updateLoggers();
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
		
		org.apache.log4j.Logger mpiLogger = getMpiLogger();
		if (mpiLogger.getAppender(MPI_APPENDER_NAME) != null) {
			mpiLogger.removeAppender(MPI_APPENDER_NAME);
		}
	}
	
	protected org.apache.log4j.Logger getMpiLogger() {
		return org.apache.log4j.Logger.getLogger(getClass().getPackage().getName());
	}
	
}
