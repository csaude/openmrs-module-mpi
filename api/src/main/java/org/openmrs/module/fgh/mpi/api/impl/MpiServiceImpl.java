package org.openmrs.module.fgh.mpi.api.impl;

import static org.openmrs.module.fgh.mpi.MpiConstants.GP_HEALTH_CENTER_ENC_TYPE_UUID;

import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fgh.mpi.MpiUtils;
import org.openmrs.module.fgh.mpi.api.MpiService;
import org.openmrs.module.fgh.mpi.api.db.MpiDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class MpiServiceImpl extends BaseOpenmrsService implements MpiService, GlobalPropertyListener {
	
	private static final Logger log = LoggerFactory.getLogger(MpiServiceImpl.class);
	
	private MpiDAO dao;
	
	private static EncounterType healthCenterEncType;
	
	/**
	 * Sets the dao
	 *
	 * @param dao the dao to set
	 */
	public void setDao(MpiDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * @see GlobalPropertyListener#supportsPropertyName(String)
	 */
	@Override
	public boolean supportsPropertyName(String propertyName) {
		return GP_HEALTH_CENTER_ENC_TYPE_UUID.equals(propertyName);
	}
	
	/**
	 * @see GlobalPropertyListener#globalPropertyChanged(GlobalProperty)
	 */
	@Override
	public void globalPropertyChanged(GlobalProperty newValue) {
		healthCenterEncType = null;
	}
	
	/**
	 * @see GlobalPropertyListener#globalPropertyDeleted(String)
	 */
	@Override
	public void globalPropertyDeleted(String propertyName) {
		healthCenterEncType = null;
	}
	
	/**
	 * @see MpiService#getMostRecentLocation(Patient)
	 */
	@Override
	public Location getMostRecentLocation(Patient patient) {
		final String encTypeUuid = MpiUtils.getGlobalPropertyValue(GP_HEALTH_CENTER_ENC_TYPE_UUID);
		if (log.isDebugEnabled()) {
			log.debug("Patient health center encounter type uuid: " + encTypeUuid);
		}
		
		if (healthCenterEncType == null) {
			EncounterType type = Context.getEncounterService().getEncounterTypeByUuid(encTypeUuid);
			if (type == null) {
				throw new APIException("No encounter found matching uuid: " + encTypeUuid);
			}
			
			healthCenterEncType = type;
		}
		
		return dao.getMostRecentLocation(patient, healthCenterEncType);
	}
	
}
