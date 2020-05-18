use queue_processing;

drop procedure if exists create_job;

delimiter //

create procedure create_job
(
  customer_id int unsigned,
  from_date date,
  to_date date,
  out job_id int unsigned
)
begin

 DECLARE validation_error CONDITION FOR SQLSTATE '45000'; 

 if customer_id is null or customer_id <= 0 then
     signal validation_error set message_text = 'Invalid value for customer_id';     
 end if; 

 if from_date is null then
     signal validation_error set message_text = 'Invalid value for from_date';
 end if; 

 if to_date is null then
     signal validation_error set message_text = 'Invalid value for to_date';
 end if; 

 insert into historical_statement_job (customer_id, from_date, to_date) values (customer_id, from_date, to_date);
 
 set job_id = last_insert_id(); 
end; //

delimiter ;
