package org.openmrs.module.fgh.mpi.api.impl;

import static org.openmrs.module.fgh.mpi.MpiConstants.GP_ADULT_PROCESS_ENC_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_CHILD_PROCESS_ENC_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiConstants.GP_FICHA_RESUMO_ENC_TYPE_UUID;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;

import java.util.List;

import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fgh.mpi.MpiUtils;
import org.openmrs.module.fgh.mpi.api.MpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class MpiServiceImpl extends BaseOpenmrsService implements MpiService {
	
	private static final Logger log = LoggerFactory.getLogger(MpiServiceImpl.class);
	
	private final static String ENC_TYPE_ID_PLACEHOLDER = "{ENC_TYPE_ID}";
	
	private final static String ENC_LOC_QUERY = "SELECT location_id FROM encounter WHERE patient_id = " + ID_PLACEHOLDER
	        + " AND encounter_type = " + ENC_TYPE_ID_PLACEHOLDER
	        + " AND voided = 0 AND location_id IS NOT NULL ORDER BY encounter_datetime ASC LIMIT 1";
	
	private static EncounterType fichaEncType;
	
	private static EncounterType adultProcessEncType;
	
	private static EncounterType childProcessEncType;
	
	/**
	 * @see MpiService#getHealthFacility(Patient)
	 */
	@Override
	public Location getHealthFacility(Patient patient) {
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
		
		location = getLocationForOldestEncounter(patient, fichaEncType);
		if (location == null) {
			if (log.isDebugEnabled()) {
				log.debug("No location found for encounter of type " + fichaEncType.getName() + ", looking up one for "
				        + "type " + adultProcessEncType.getName());
			}
			
			location = getLocationForOldestEncounter(patient, adultProcessEncType);
			if (location == null) {
				if (log.isDebugEnabled()) {
					log.debug("No location found for encounter of type " + adultProcessEncType.getName()
					        + ", looking up one for type " + childProcessEncType.getName());
				}
				
				location = getLocationForOldestEncounter(patient, childProcessEncType);
			}
		}
		
		return location;
	}
	
	@Override
	public Location getLocationForOldestEncounter(Patient patient, EncounterType type) {
		final String query = ENC_LOC_QUERY.replace(ID_PLACEHOLDER, patient.getId().toString())
		        .replace(ENC_TYPE_ID_PLACEHOLDER, type.getId().toString());
		
		List<List<Object>> ids = MpiUtils.executeQuery(query);
		if (ids.isEmpty()) {
			return null;
		}
		
		return Context.getLocationService().getLocation(Integer.valueOf(ids.get(0).get(0).toString()));
	}
	
}
