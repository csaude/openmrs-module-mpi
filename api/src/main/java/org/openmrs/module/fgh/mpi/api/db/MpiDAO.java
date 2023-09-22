package org.openmrs.module.fgh.mpi.api.db;

import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;

/**
 * Provides a DAO layer for the module
 */
public interface MpiDAO {
	
	/**
	 * Gets the location associated to the specified patient's most recent encounter matching the
	 * specified encounter type.
	 *
	 * @param patient the patient to match
	 * @param encounterType the encounter type to match
	 * @return the most recent location
	 */
	Location getMostRecentLocation(Patient patient, EncounterType encounterType);
	
}
