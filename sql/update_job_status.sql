use queue_processing;

drop procedure if exists update_job_status;

delimiter //

create procedure update_job_status
(
  job_id int unsigned,
  current_status varchar(255),
  new_status varchar(255),
  out updated_count int 
)
begin

 DECLARE validation_error CONDITION FOR SQLSTATE '45000'; 

 if job_id is null or job_id <=0 then
     signal validation_error set message_text = 'Invalid value for job_id';
 end if; 
 
 if current_status is null then
     signal validation_error set message_text = 'Invalid value for current_status';     
 end if; 


 if new_status is null then
     signal validation_error set message_text = 'Invalid value for new_status';     
 end if; 

 update historical_statement_job set status = new_status
 where id = job_id and 
 status = current_status;

 set updated_count = row_count();

end; //

delimiter ;
