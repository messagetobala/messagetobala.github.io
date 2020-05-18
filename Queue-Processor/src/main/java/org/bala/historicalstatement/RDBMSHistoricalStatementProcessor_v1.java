package org.bala.historicalstatement;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bala.historicalstatement.dao.HistoricalStatementDAO;
import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.bala.historicalstatement.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class RDBMSHistoricalStatementProcessor_v1 {

	private static final Logger logger = LoggerFactory.getLogger(RDBMSHistoricalStatementProcessor_v1.class);
	private static final int BATCH_SIZE = 50;
	
	@Autowired
	private HistoricalStatementDAO dao;
	
	@Autowired
	private HistoricalStatementWorker_v1 worker;
	
	private ExecutorService executor =  Executors.newFixedThreadPool(BATCH_SIZE);
	
	private  boolean running = true;
	
	public void processHistoricalStatementJobs() {
		
		while(running) {
			try {
				logger.info("Fetching historical statement jobs to process ");
				List<HistoricalStatementJob> jobs = dao.getJobListToProcess(BATCH_SIZE);
				
				for (HistoricalStatementJob job : jobs) {
					if (!running) {
						break;
					}
					if (acquireLock(job)) {
						//Lock acquired
						executor.submit(new Runnable() {

							@Override
							public void run() {
								try {
									worker.processJob(job);
								} catch (HistoricalStatementException e) {
									logger.error("Error processing job {}",job.getJobId(), e);
								}
								
							}
							
						});
					}
				}
				
				if (jobs.isEmpty()) {
					logger.info("No pending jobs found for processing. Sleeping for a second");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
				}
				
			} catch(HistoricalStatementException e) {
				logger.error("Error processing jobs",e);
			}

		}
		
		executor.shutdown();
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Interrupted while awaiting termination of thereads.", e);
		}
	}
	
	private boolean acquireLock(HistoricalStatementJob job) {
		try {
			if (dao.updateJobStatus(job.getJobId(), JobStatus.SUBMITTED, JobStatus.IN_PROGRESS) == 1) {
				return true;
			}
		} catch (HistoricalStatementException e) {
			logger.error("Error trying to acquire lock for job id {}", job.getJobId(), e);
		}
		
		return false;
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}
	public static void main(String[] args) {
		
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/application.xml");
		
		RDBMSHistoricalStatementProcessor_v1 processor = ctx.getBean(RDBMSHistoricalStatementProcessor_v1.class);
		
		processor.processHistoricalStatementJobs();
		
	}

}
