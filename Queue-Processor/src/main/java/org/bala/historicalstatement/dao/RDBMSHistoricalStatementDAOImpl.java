package org.bala.historicalstatement.dao;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.bala.historicalstatement.model.HistoricalStatementException;
import org.bala.historicalstatement.model.HistoricalStatementJob;
import org.bala.historicalstatement.model.JobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

public class RDBMSHistoricalStatementDAOImpl implements HistoricalStatementDAO {
	
	private SimpleJdbcCall createJobCall;
	private SimpleJdbcCall getJobListCall;
	private SimpleJdbcCall updateJobStatusCall;
	private SimpleJdbcCall getJobToCall;
	private SimpleJdbcCall getJobWithIdCall;
	
	
	@Autowired
	public RDBMSHistoricalStatementDAOImpl(DataSource dataSource) {
		createJobCall = new SimpleJdbcCall(dataSource).withProcedureName("create_job");
		
		getJobListCall = new SimpleJdbcCall(dataSource).withProcedureName("get_job_list_to_process")
				.returningResultSet("job_list_to_process", new HistoricalStatementJobMapper());
			
		updateJobStatusCall = new SimpleJdbcCall(dataSource).withProcedureName("update_job_status");
		
		getJobToCall = new SimpleJdbcCall(dataSource).withProcedureName("get_job_to_process")
				.returningResultSet("job_to_process", new HistoricalStatementJobMapper());
		
		getJobWithIdCall = new SimpleJdbcCall(dataSource).withProcedureName("get_job_with_id")
				.returningResultSet("job_with_id", new HistoricalStatementJobMapper());
		
	}

	@Override
	public long createJob(HistoricalStatementJob job) throws HistoricalStatementException {
		
		SqlParameterSource in = new MapSqlParameterSource().addValue("customer_id", job.getCustomerId())
				.addValue("from_date", job.getFromDate())
				.addValue("to_date", job.getToDate());
				
				
		
		Map<String, Object> output = createJobCall.execute(in);
		
		
		return (long) output.get("job_id");
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HistoricalStatementJob> getJobListToProcess(int batchSize) throws HistoricalStatementException {
		SqlParameterSource in = new MapSqlParameterSource()
				.addValue("batch_size", batchSize);
				
				
		
		Map<String, Object> output = getJobListCall.execute(in);
		
		
		return (List<HistoricalStatementJob>) output.get("job_list_to_process");
	}

	@Override
	public int updateJobStatus(long jobId, JobStatus currentStatus, JobStatus newStatus) throws HistoricalStatementException {
		SqlParameterSource in = new MapSqlParameterSource().addValue("job_id", jobId)
				.addValue("new_status", newStatus.toString())
				.addValue("current_status", currentStatus.toString());
		
		Map<String, Object> output = updateJobStatusCall.execute(in);
		return (int) output.get("updated_count");	
	}

	@SuppressWarnings("unchecked")
	@Override
	public HistoricalStatementJob getJobToProcess() throws HistoricalStatementException {
		Map<String, Object> output = getJobToCall.execute();
		List<HistoricalStatementJob> jobs = (List<HistoricalStatementJob>) output.get("job_to_process");
		if (jobs.size() > 0) {
			return jobs.get(0);
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public HistoricalStatementJob getJobWithId(long jobId) throws HistoricalStatementException {
		SqlParameterSource in = new MapSqlParameterSource().addValue("job_id", jobId);

		Map<String, Object> output = getJobWithIdCall.execute(in);
		
		List<HistoricalStatementJob> jobs = (List<HistoricalStatementJob>) output.get("job_with_id");
		if (jobs.size() > 0) {
			return jobs.get(0);
		} else {
			return null;
		}
	}

}
