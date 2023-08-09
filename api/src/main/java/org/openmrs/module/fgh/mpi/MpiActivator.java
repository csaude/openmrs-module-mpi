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
import java.io.IOException;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.PatternLayout;
import org.openmrs.api.APIException;
import org.openmrs.module.BaseModuleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiActivator extends BaseModuleActivator {
	
	private static final Logger log = LoggerFactory.getLogger(MpiActivator.class);
	
	protected static final String MPI_APPENDER_NAME = "MPI_APPENDER";
	
	protected static final String LAYOUT = "%-5p %t - %C{1}.%M(%L) |%d{ISO8601}| %m%n";
	
	/**
	 * @see BaseModuleActivator#started()
	 */
	@Override
	public void started() {
		log.info("MPI module started");
		log.info("Adding MPI log file to log4j configuration");
		
		File mpiLogFile = MpiUtils.createPath(getApplicationDataDirectory(), "mpi", "logs", "mpi.log").toFile();
		
		try {
			DailyRollingFileAppender mpiAppender = new DailyRollingFileAppender(new PatternLayout(LAYOUT),
			        mpiLogFile.getAbsolutePath(), "'.'yyyy-MM-dd");
			mpiAppender.setName(MPI_APPENDER_NAME);
			org.apache.log4j.Logger mpiLogger = getMpiLogger();
			mpiLogger.setAdditivity(false);
			mpiLogger.addAppender(mpiAppender);
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
