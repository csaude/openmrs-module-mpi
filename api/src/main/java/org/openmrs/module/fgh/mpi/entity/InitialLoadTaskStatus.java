package org.openmrs.module.fgh.mpi.entity;

import java.util.Date;

public class InitialLoadTaskStatus {
	
	private Integer id;
	
	private boolean running;
	
	private Integer patientOffsetId;
	
	private Date startDate;
	
	private Date endDate;
	
	private boolean locked;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	public Integer getPatientOffsetId() {
		return patientOffsetId;
	}
	
	public void setPatientOffsetId(Integer patientOffsetId) {
		this.patientOffsetId = patientOffsetId;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public Date getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}
