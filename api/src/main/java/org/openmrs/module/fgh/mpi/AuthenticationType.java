package org.openmrs.module.fgh.mpi;

public enum AuthenticationType {
	
	OAUTH,
	CERTIFICATE;
	
	public boolean isOuath() {
		return this.equals(OAUTH);
	}
	
	public boolean isCertificate() {
		return this.equals(CERTIFICATE);
	}
}
