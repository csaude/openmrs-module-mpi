package org.openmrs.module.fgh.mpi.api;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;

/**
 * Provides a service layer for the module
 */
public interface MpiService extends OpenmrsService {
	
	/**
	 * Gets the location associated to a patient's most recent encounter of type Ficha Resumo or
	 * clinical process with Ficha Resumo having higer priority.
	 * 
	 * @param patient the patient to match
	 * @return the most recent location
	 */
	Location getMostRecentLocation(Patient patient);
	
}
