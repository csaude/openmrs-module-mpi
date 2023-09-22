package org.openmrs.module.fgh.mpi.api.impl;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fgh.mpi.api.MpiService;
import org.openmrs.module.fgh.mpi.api.db.MpiDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class MpiServiceImpl extends BaseOpenmrsService implements MpiService {
	
	private static final Logger log = LoggerFactory.getLogger(MpiServiceImpl.class);
	
	private MpiDAO dao;
	
	/**
	 * Sets the dao
	 *
	 * @param dao the dao to set
	 */
	public void setDao(MpiDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * @see MpiService#getMostRecentLocation(Patient)
	 */
	@Override
	public Location getMostRecentLocation(Patient patient) {
		return null;
	}
	
}
