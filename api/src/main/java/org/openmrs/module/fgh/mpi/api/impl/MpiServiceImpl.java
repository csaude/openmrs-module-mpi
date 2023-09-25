package org.openmrs.module.fgh.mpi.api.impl;

import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fgh.mpi.MpiConstants;
import org.openmrs.module.fgh.mpi.MpiUtils;
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
		//Add global property listener
		final String encTypeUuid = MpiUtils.getGlobalPropertyValue(MpiConstants.GP_HEALTH_CENTER_ENC_TYPE_UUID);
		if (log.isDebugEnabled()) {
			log.debug("Patient health center encounter type uuid: " + encTypeUuid);
		}
		
		EncounterType type = Context.getEncounterService().getEncounterTypeByUuid(encTypeUuid);
		if (type == null) {
			throw new APIException("No encounter found matching uuid: " + encTypeUuid);
		}
		
		return dao.getMostRecentLocation(patient, type);
	}
	
}
