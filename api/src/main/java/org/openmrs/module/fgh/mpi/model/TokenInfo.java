package org.openmrs.module.fgh.mpi.model;

import org.apache.commons.lang.time.StopWatch;

public class TokenInfo {
	
	private String access_token;
	
	private String id_token;
	
	private String token_type;
	
	private Double expires_in;
	
	private String refresh_token;
	
	private StopWatch stopWach;
	
	public TokenInfo() {
	}
	
	public String getAccess_token() {
		return access_token;
	}
	
	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}
	
	public String getId_token() {
		return id_token;
	}
	
	public void setId_token(String id_token) {
		this.id_token = id_token;
	}
	
	public String getToken_type() {
		return token_type;
	}
	
	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}
	
	public Double getExpires_in() {
		return expires_in;
	}
	
	public void setExpires_in(Double expires_in) {
		this.expires_in = expires_in;
	}
	
	public String getRefresh_token() {
		return refresh_token;
	}
	
	public void setRefresh_token(String refresh_token) {
		this.refresh_token = refresh_token;
	}
	
	public boolean isValid() {
		if (this.stopWach == null)
			return false;
		
		this.stopWach.split();
		
		double takenTime = this.stopWach.getSplitTime();
		
		return takenTime + 30000 < this.expires_in;
	}
	
	public void timeCountDown() {
		this.stopWach = new StopWatch();
		
		this.stopWach.start();
	}
	
}
