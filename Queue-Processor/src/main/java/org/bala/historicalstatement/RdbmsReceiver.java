package org.bala.historicalstatement;

import java.util.Date;

import org.bala.historicalstatement.dao.HistoricalStatementDAO;
import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RdbmsReceiver {

	private static final Logger logger = LoggerFactory.getLogger(RdbmsReceiver.class);
	
	public static void main(String[] args) throws HistoricalStatementException {
		
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/application.xml");
		HistoricalStatementDAO dao = (HistoricalStatementDAO) ctx.getBean("rdbmsDao");
		
		logger.info("Submitting 100 historical statement jobs.");
	
		//Create 10 K jobs.
		for (int i = 0; i < 100; i++) {
			dao.createJob(getJob());
		}
		ctx.close();
	}
	
	private static HistoricalStatementJob getJob() {
		HistoricalStatementJob job = new HistoricalStatementJob();
		job.setCustomerId(1);
		job.setFromDate(new Date());
		job.setToDate(new Date());
		
		return job;
		
	}
}
