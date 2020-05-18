use queue_processing;

drop procedure if exists get_job_to_process;

delimiter //

create procedure get_job_to_process()
begin
 select id, customer_id, from_date, to_date, status from  historical_statement_job
 where status = 'submitted' 
 limit 1
 for update
 skip locked;
end; //

delimiter ;
