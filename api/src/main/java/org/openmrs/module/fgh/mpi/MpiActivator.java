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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.PatternLayout;
import org.openmrs.api.APIException;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiActivator extends BaseModuleActivator {
	
	private static final Logger log = LoggerFactory.getLogger(MpiActivator.class);
	
	private static final String MPI_APPENDER_NAME = "MPI_APPENDER";
	
	/**
	 * @see BaseModuleActivator#started()
	 */
	@Override
	public void started() {
		log.info("MPI module started");
		log.info("Registering MPI log appender");
		
		org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender consoleAppender = (ConsoleAppender) rootLogger.getAppender("CONSOLE");
		PatternLayout layout = new PatternLayout("%-5p %t - %C{1}.%M(%L) |%d{ISO8601}| %m%n");
		consoleAppender.setLayout(layout);
		consoleAppender.activateOptions();
		
		File mpiLogFile = Paths.get(OpenmrsUtil.getApplicationDataDirectory(), "mpi", "logs", "mpi.log").toFile();
		try {
			DailyRollingFileAppender mpiAppender = new DailyRollingFileAppender(layout, mpiLogFile.getAbsolutePath(),
			        "'.'yyyy-MM-dd");
			mpiAppender.setName(MPI_APPENDER_NAME);
			rootLogger.addAppender(mpiAppender);
			org.apache.log4j.Logger.getLogger("org.openmrs.module.debezium").addAppender(mpiAppender);
			org.apache.log4j.Logger.getLogger("org.openmrs.module.fgh.mpi").addAppender(mpiAppender);
		}
		catch (IOException e) {
			throw new APIException(e);
		}
		
	}
	
	/**
	 * @see BaseModuleActivator#stopped()
	 */
	@Override
	public void stopped() {
		log.info("MPI module stopped");
		log.info("Removing MPI log appender");
		
		org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
		if (rootLogger.getAppender(MPI_APPENDER_NAME) != null) {
			rootLogger.removeAppender(MPI_APPENDER_NAME);
		}
	}
	
}
