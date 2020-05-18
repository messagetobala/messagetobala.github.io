use queue_processing;
create table historical_statement_job (
  id int unsigned auto_increment primary key,
  customer_id int unsigned NOT NULL,
  from_date date  NOT NULL,
  to_date date  NOT NULL,
  status enum ('submitted', 'in_progress', 'completed', 'failed') default ('submitted'),
  updated_at timestamp default current_timestamp on update current_timestamp,
  created_at timestamp default current_timestamp
);
