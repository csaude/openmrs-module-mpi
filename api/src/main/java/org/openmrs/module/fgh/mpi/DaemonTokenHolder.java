package org.openmrs.module.fgh.mpi;

import org.openmrs.module.DaemonToken;

/**
 * Holder for DaemonToken
 */
public class DaemonTokenHolder {
	
	private static DaemonToken daemonToken;
	
	/**
	 * Returns the token
	 * 
	 * @return daemon token
	 */
	public static DaemonToken getToken() {
		return daemonToken;
	}
	
	/**
	 * Sets the daemon token
	 * 
	 * @param token the token to set
	 */
	public static void setToken(DaemonToken token) {
		daemonToken = token;
	}
	
}
