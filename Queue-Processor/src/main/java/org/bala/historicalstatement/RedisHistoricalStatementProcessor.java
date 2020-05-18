package org.bala.historicalstatement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bala.historicalstatement.model.HistoricalStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;



public class RedisHistoricalStatementProcessor {

	private static final Logger logger = LoggerFactory.getLogger(RedisHistoricalStatementProcessor.class);
	private static final int BATCH_SIZE = 50;
	
	@Autowired
	private RedisHistoricalStatementWorker worker;	
	
	private ExecutorService executor =  Executors.newFixedThreadPool(BATCH_SIZE);
	
	public void processHistoricalStatementJobs() {
		
		for (int i = 0; i < BATCH_SIZE; i++) {
			logger.info("Starting thread {} to process historical statement jobs" , i+1);
			executor.submit(new Runnable() {

				@Override
				public void run() {
					worker.processJobs();
					
				}
				
			});
		}
	}
	

	public static void main(String[] args) {
		
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/application.xml");
		
		RedisHistoricalStatementProcessor processor = ctx.getBean(RedisHistoricalStatementProcessor.class);
		
		processor.processHistoricalStatementJobs();
		
		//ctx.close();
		
	}
	
	
}
