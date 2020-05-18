package org.bala.historicalstatement;

import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;

public interface HistoricalStatementService {

	public long createJob(HistoricalStatementJob job) throws HistoricalStatementException;
	
	public void processJob(HistoricalStatementJob job) throws HistoricalStatementException;
	
}
