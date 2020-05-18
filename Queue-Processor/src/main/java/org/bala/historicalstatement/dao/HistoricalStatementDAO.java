package org.bala.historicalstatement.dao;

import java.util.List;

import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.bala.historicalstatement.model.JobStatus;

public interface HistoricalStatementDAO {

	public long createJob(HistoricalStatementJob job) throws HistoricalStatementException;
		
	public List<HistoricalStatementJob> getJobListToProcess(int batchSize) throws HistoricalStatementException;
	
	public int updateJobStatus(long jobId, JobStatus currentStatus, JobStatus newStatus) throws HistoricalStatementException;
	
	public HistoricalStatementJob getJobToProcess() throws HistoricalStatementException;
	
	public HistoricalStatementJob getJobWithId(long jobId) throws HistoricalStatementException;
}
