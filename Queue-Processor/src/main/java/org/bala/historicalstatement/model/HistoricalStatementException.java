package org.bala.historicalstatement.model;

public class HistoricalStatementException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 449141628830730627L;

	private boolean isPermFailure;

	public boolean isPermFailure() {
		return isPermFailure;
	}

	public void setPermFailure(boolean isPermFailure) {
		this.isPermFailure = isPermFailure;
	}
	
	
}
