package org.bala.historicalstatement;

import org.bala.historicalstatement.dao.HistoricalStatementDAO;
import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.bala.historicalstatement.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class HistoricalStatementWorker_v2 {

	private static final Logger logger = LoggerFactory.getLogger(HistoricalStatementWorker_v2.class);
	
	private HistoricalStatementDAO dao;

	@Autowired
	public HistoricalStatementWorker_v2(HistoricalStatementDAO dao) {
		this.dao = dao;
	}

	private void doWork(HistoricalStatementJob job) throws HistoricalStatementException {
		//Do actual work
	}	

	@Transactional(rollbackFor = HistoricalStatementException.class)
	public HistoricalStatementJob processJob() throws HistoricalStatementException {
		HistoricalStatementJob job = dao.getJobToProcess();
		logger.info("Processing job {} ", job.getJobId());
		if (job != null) {
			try {
				dao.updateJobStatus(job.getJobId(), JobStatus.SUBMITTED, JobStatus.IN_PROGRESS);
				doWork(job);
				//Update status to completed.
				dao.updateJobStatus(job.getJobId(), JobStatus.IN_PROGRESS, JobStatus.COMPLETED);
			} catch(HistoricalStatementException e) {
				if (e.isPermFailure()) {
					dao.updateJobStatus(job.getJobId(), JobStatus.IN_PROGRESS, JobStatus.FAILED);
				}
				//otherwise will be retried.
			}	
		}
		return job;
	}
}
