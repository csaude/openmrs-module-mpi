package org.openmrs.module.fgh.mpi.api;

import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;

/**
 * Provides a service layer for the module
 */
public interface MpiService extends OpenmrsService {
	
	/**
	 * Gets the location associated to a patient's oldest encounter of type Ficha Resumo or clinical
	 * process with Ficha Resumo having higher priority.
	 * 
	 * @param patient the patient to match
	 * @return the location of the health facility
	 */
	Location getHealthFacility(Patient patient);
	
	/**
	 * Gets the location of the oldest encounter for the patient matching the specified patient and
	 * encounter type ids.
	 *
	 * @param patient the patient to match
	 * @param type the encounter type to match
	 * @return the oldest encounter location
	 */
	Location getLocationForOldestEncounter(Patient patient, EncounterType type);
	
}
