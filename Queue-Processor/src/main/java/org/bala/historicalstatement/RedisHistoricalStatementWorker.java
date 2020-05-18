package org.bala.historicalstatement;


import org.bala.historicalstatement.dao.HistoricalStatementDAO;
import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.bala.historicalstatement.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;

public class RedisHistoricalStatementWorker {

	private static final Logger logger = LoggerFactory.getLogger(RedisHistoricalStatementWorker.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	@Qualifier("redisHost")	
	private String redisHost;
	
	@Autowired
	@Qualifier("redisPort")	
	private String redisPort;
	
	@Autowired
	private HistoricalStatementDAO dao;
	
	private  boolean running = true;

	public void processJobs() {
		
		Jedis jedis = new Jedis(redisHost, Integer.parseInt(redisPort));
		
		while (running) {
			String jobStr = jedis.brpoplpush("pending_jobs", "in_progress_jobs", 5000);
			if (jobStr != null) {
				HistoricalStatementJob job = null;	
				try {
					job = (HistoricalStatementJob) mapper.readValue(jobStr, HistoricalStatementJob.class);
					Long jobId = job.getJobId();
					processJob(jobId);
					jedis.lrem("in_progress_jobs", 1, jobStr);
				} catch (HistoricalStatementException e){
					if (e.isPermFailure()) {
						try {
							dao.updateJobStatus(job.getJobId(), JobStatus.IN_PROGRESS, JobStatus.FAILED);
						} catch (HistoricalStatementException t) {
							logger.error("Error processing job.", t.getMessage());
						}
						
					}
			    } catch (Exception e) {
					logger.error("Error processing job {}", jobStr, e);
				}				
			} else {
				logger.info("No jobs found to process");
			}	
		}
		
		jedis.close();
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
	
	private void processJob(long jobId) throws HistoricalStatementException {

		logger.info("Processing job {}", jobId);
				
		//Do actual work		
	}		

}
