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
	
	public void setAccessToken(String access_token) {
		this.accessToken = access_token;
	}
	
	public String getTokenId() {
		return tokenId;
	}
	
	public void setTokenId(String id_token) {
		this.tokenId = id_token;
	}
	
	public String getTokenType() {
		return tokenType;
	}
	
	public void setTokenType(String token_type) {
		this.tokenType = token_type;
	}
	
	public Double getExpiresIn() {
		return expiresIn;
	}
	
	public void setExpiresIn(Double expires_in) {
		this.expiresIn = expires_in;
	}
	
	public String getRefreshToken() {
		return refreshToken;
	}
	
	public void setRefreshToken(String refresh_token) {
		this.refreshToken = refresh_token;
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
	
}
