package org.openmrs.module.fgh.mpi.entity;

import java.util.Date;

public class InitialLoadTaskStatus {
	
	private Integer id;
	
	private boolean isRunning;
	
	private Integer patientOffsetId;
	
	private Date startDate;
	
	private Date endDate;
	
	private boolean isActive;
	
	private boolean isLocked;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public void setRunning(boolean running) {
		isRunning = running;
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
	
	public boolean isActive() {
		return isActive;
	}
	
	public void setActive(boolean active) {
		isActive = active;
	}
	
	public boolean isLocked() {
		return isLocked;
	}
	
	public void setLocked(boolean locked) {
		isLocked = locked;
	}
}
