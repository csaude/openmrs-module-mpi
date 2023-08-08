package org.openmrs.module.fgh.mpi;

public enum MpiSystemType {
	
	SANTEMPI, OPENCR;
	
	public boolean isSanteMPI() {
		return this.equals(SANTEMPI);
	}
	
	public boolean isOpenCr() {
		return this.equals(OPENCR);
	}
}
