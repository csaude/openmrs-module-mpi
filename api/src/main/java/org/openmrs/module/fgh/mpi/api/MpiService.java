package org.openmrs.module.fgh.mpi.api;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;

/**
 * Provides a service layer for the module
 */
public interface MpiService extends OpenmrsService {
	
	/**
	 * Gets the location associated to a patient's most recent encounter of the type matching the uuid
	 * specified via the {@link org.openmrs.module.fgh.mpi.MpiConstants#GP_FICHA_RESUMO_ENC_TYPE_UUID}
	 * global property.
	 * 
	 * @param patient the patient to match
	 * @return the most recent location
	 */
	Location getMostRecentLocation(Patient patient);
	
}
