package org.openmrs.module.fgh.mpi.api.impl;

import static org.openmrs.module.fgh.mpi.MpiConstants.GP_ADULT_PROCESS_ENC_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_CHILD_PROCESS_ENC_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_FICHA_RESUMO_ENC_TYPE_UUID;

import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.impl.BaseOpenmrsService;
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
	
	private static EncounterType fichaEncType;
	
	private static EncounterType adultProcessEncType;
	
	private static EncounterType childProcessEncType;
	
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
		Location location;
		if (fichaEncType == null) {
			fichaEncType = MpiUtils.getEncounterTypeByGlobalProperty(GP_FICHA_RESUMO_ENC_TYPE_UUID);
		}
		
		if (adultProcessEncType == null) {
			adultProcessEncType = MpiUtils.getEncounterTypeByGlobalProperty(GP_ADULT_PROCESS_ENC_TYPE_UUID);
		}
		
		if (childProcessEncType == null) {
			childProcessEncType = MpiUtils.getEncounterTypeByGlobalProperty(GP_CHILD_PROCESS_ENC_TYPE_UUID);
		}
		
		location = dao.getMostRecentLocation(patient, fichaEncType);
		if (location == null) {
			if (log.isDebugEnabled()) {
				log.debug("No location found for encounter of type " + fichaEncType.getName() + ", looking up one for "
				        + "type " + adultProcessEncType.getName());
			}
			
			location = dao.getMostRecentLocation(patient, adultProcessEncType);
			if (location == null) {
				if (log.isDebugEnabled()) {
					log.debug("No location found for encounter of type " + adultProcessEncType.getName()
					        + ", looking up one for type " + childProcessEncType.getName());
				}
				
				location = dao.getMostRecentLocation(patient, childProcessEncType);
			}
		}
		
		return location;
	}
	
}
