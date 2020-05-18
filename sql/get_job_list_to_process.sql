use queue_processing;

drop procedure if exists get_job_list_to_process;

delimiter //

create procedure get_job_list_to_process
(
  batch_size int
)
begin

 DECLARE validation_error CONDITION FOR SQLSTATE '45000'; 

 if batch_size is null or batch_size <= 0 then
     signal validation_error set message_text = 'Invalid value for batch_size';     
 end if; 

 -- update status of of any job which is 'in_progress' state for 5 minutes or more
 update historical_statement_job set status = 'submitted'
 where status = 'in_progress' and
 timestampdiff(minute, updated_at, now()) >=5;

 select id, customer_id, from_date, to_date, status from  historical_statement_job
 where status = 'submitted'
 order by id asc
 limit batch_size; 
end; //

delimiter ;
