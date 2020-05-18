package org.bala.historicalstatement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class RDBMSHistoricalStatementProcessor_v2 {

	private static final Logger logger = LoggerFactory.getLogger(RDBMSHistoricalStatementProcessor_v2.class);
	private static final int BATCH_SIZE = 50;
	
	@Autowired
	private HistoricalStatementWorker_v2 worker;	

	private  boolean running = true;
	
	private ExecutorService executor =  Executors.newFixedThreadPool(BATCH_SIZE);
	
	public void processHistoricalStatementJobs() {
		
		for (int i = 0; i < BATCH_SIZE; i++) {
			logger.info("Starting thread {} to process historical statement jobs" , i+1);
			executor.submit(new Runnable() {

				@Override
				public void run() {
					while (running) {
						try {
							HistoricalStatementJob job = worker.processJob();
							if (job == null) {
								try {
									logger.info("No pending jobs found for processing. Sleeping for a second");
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									break;
								}
							}							
						} catch (HistoricalStatementException e) {
							logger.error("Error processing job", e);
						}
					}
					
					
				}
				
			});
		}
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	public static void main(String[] args) {
		
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/application.xml");
		
		RDBMSHistoricalStatementProcessor_v2 processor = ctx.getBean(RDBMSHistoricalStatementProcessor_v2.class);
		
		processor.processHistoricalStatementJobs();
				
	}
}
