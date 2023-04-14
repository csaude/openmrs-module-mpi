package org.openmrs.module.fgh.mpi.miscellaneous;

public enum AuthenticationType {
	OAUTH,
	SSL;
	
	public boolean isOuath() {
		return this.equals(OAUTH);
	}
	
	public boolean isSsl() {
		return this.equals(SSL);
	}
}
