package org.bala.historicalstatement.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.bala.historicalstatement.model.JobStatus;
import org.springframework.jdbc.core.RowMapper;

public class HistoricalStatementJobMapper implements RowMapper<HistoricalStatementJob> {

	@Override
	public HistoricalStatementJob mapRow(ResultSet rs, int rowNum) throws SQLException {

		HistoricalStatementJob job = new HistoricalStatementJob();
		
		job.setJobId(rs.getInt("id"));
		job.setCustomerId(rs.getInt("customer_id"));
		job.setFromDate(rs.getDate("from_date"));
		job.setToDate(rs.getDate("to_date"));
		job.setStatus(JobStatus.valueOf(rs.getString("status").toUpperCase()));
		
		return job;
	}

	
}
