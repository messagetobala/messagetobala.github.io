package org.bala.historicalstatement;

import java.util.Date;

import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;

public class RedisReceiver {

	private static final Logger logger = LoggerFactory.getLogger(RedisReceiver.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static void main(String[] args) {
		
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/application.xml");		
		String redisHost = (String) ctx.getBean("redisHost");
		String redisPort = (String) ctx.getBean("redisPort");
		
		Jedis jedis = new Jedis(redisHost, Integer.parseInt(redisPort));
		
		logger.info("Submitting 100 historical statement jobs.");
		for (int i = 0; i < 100; i++) {
			HistoricalStatementJob job = getJob(i);
			try {		
					jedis.lpush("pending_jobs", mapper.writeValueAsString(job));
			} catch (Exception e) {
				logger.error("Error creating job {} ", job.getJobId(), e);
			}
		}
		
		jedis.close();
		ctx.close();
	}
	
	private static HistoricalStatementJob getJob(int i) {
		HistoricalStatementJob job = new HistoricalStatementJob();
		job.setJobId(i);
		job.setCustomerId(1);
		job.setFromDate(new Date());
		job.setToDate(new Date());
		return job;
		
	}	
}
