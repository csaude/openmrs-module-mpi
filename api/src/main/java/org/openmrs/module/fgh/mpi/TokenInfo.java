package org.openmrs.module.fgh.mpi;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenInfo implements Serializable {
	
	@JsonProperty("access_token")
	private String accessToken;
	
	@JsonProperty("id_token")
	private String tokenId;
	
	@JsonProperty("token_type")
	private String tokenType;
	
	@JsonProperty("expires_in")
	private long expiresIn;
	
	@JsonProperty("refresh_token")
	private String refreshToken;
	
	private LocalDateTime tokenExpirationDateTime;
	
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
	
	public long getExpiresIn() {
		return expiresIn;
	}
	
	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}
	
	public String getRefreshToken() {
		return refreshToken;
	}
	
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public void setTokenExpirationDateTime(LocalDateTime tokenExpirationDateTime) {
		this.tokenExpirationDateTime = tokenExpirationDateTime;
	}

	public LocalDateTime getTokenExpirationDateTime() {
		return tokenExpirationDateTime;
	}

	public boolean isValid(LocalDateTime tokenDateTime) {
		return tokenDateTime.isBefore(tokenExpirationDateTime);
	}
}
