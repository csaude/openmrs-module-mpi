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

import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiActivator extends BaseModuleActivator implements DaemonTokenAware {
	
	private static final Logger log = LoggerFactory.getLogger(MpiActivator.class);
	
	/**
	 * @see BaseModuleActivator#started()
	 */
	@Override
	public void started() {
		log.info("MPI module started");
	}
	
	/**
	 * @see BaseModuleActivator#stopped()
	 */
	@Override
	public void stopped() {
		log.info("MPI module stopped");
	}
	
	@Override
	public void setDaemonToken(DaemonToken daemonToken) {
		DaemonTokenHolder.setToken(daemonToken);
	}
	
}