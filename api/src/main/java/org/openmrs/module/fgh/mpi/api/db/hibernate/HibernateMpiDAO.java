package org.openmrs.module.fgh.mpi.api.db.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.fgh.mpi.api.db.MpiDAO;

public class HibernateMpiDAO implements MpiDAO {
	
	private SessionFactory sessionFactory;
	
	/**
	 * Sets the sessionFactory
	 *
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}
	
	@Override
	public Location getMostRecentLocation(Patient patient, EncounterType encounterType) {
		return null;
	}
	
}
