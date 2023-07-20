package org.openmrs.module.fgh.mpi;

import org.apache.commons.lang.time.StopWatch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenInfo {
	
	@JsonProperty("access_token")
	private String accessToken;
	
	@JsonProperty("id_token")
	private String tokenId;
	
	@JsonProperty("token_type")
	private String tokenType;
	
	@JsonProperty("expires_in")
	private Double expiresIn;
	
	@JsonProperty("refresh_token")
	private String refreshToken;
	
	private StopWatch stopWach;
	
	public TokenInfo() {
	}
	
	public String getAccessToken() {
		return accessToken;
	}
	
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	
	public String getTokenId() {
		return tokenId;
	}
	
	public void setTokenId(String idToken) {
		this.tokenId = idToken;
	}
	
	public String getTokenType() {
		return tokenType;
	}
	
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	
	public Double getExpiresIn() {
		return expiresIn;
	}
	
	public void setExpiresIn(Double expiresIn) {
		this.expiresIn = expiresIn;
	}
	
	public String getRefreshToken() {
		return refreshToken;
	}
	
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
	public boolean isValid() {
		if (this.stopWach == null)
			return false;
		
		this.stopWach.split();
		
		double takenTime = this.stopWach.getSplitTime();
		
		return takenTime + 30000 < this.expiresIn;
	}
	
	public void timeCountDown() {
		this.stopWach = new StopWatch();
		
		this.stopWach.start();
	}
	
	public void setStopWach(StopWatch stopWach) {
		this.stopWach = stopWach;
	}
}
