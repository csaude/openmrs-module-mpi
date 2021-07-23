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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiActivator extends BaseModuleActivator {
	
	private static final Logger log = LoggerFactory.getLogger(MpiActivator.class);
	
	/**
	 * @see BaseModuleActivator#contextRefreshed()
	 */
	@Override
	public void contextRefreshed() {
		log.info("Starting OpenMRS debezium engine after context refresh");
		
		DebeziumEngineManager.start();
	}
	
	/**
	 * @see BaseModuleActivator#willRefreshContext()
	 */
	@Override
	public void willRefreshContext() {
		log.info("Stopping OpenMRS debezium engine before context refresh");
		
		DebeziumEngineManager.stop();
	}
	
	/**
	 * @see BaseModuleActivator#willStop()
	 */
	@Override
	public void willStop() {
		log.info("Stopping OpenMRS debezium engine before MPI module is stopped");
		
		DebeziumEngineManager.stop();
	}
	
}
