package org.openmrs.module.fgh.mpi.api.db.hibernate;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Encounter;
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
		Criteria criteria = getCurrentSession().createCriteria(Encounter.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("encounterType", encounterType));
		criteria.add(Restrictions.isNotNull("location"));
		criteria.add(Restrictions.eq("voided", false));
		criteria.setProjection(Projections.property("location"));
		criteria.addOrder(Order.asc("encounterDatetime"));
		criteria.setMaxResults(1);
		
		return (Location) criteria.uniqueResult();
	}
	
}
