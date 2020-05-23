---
layout: post
title:  Implementing "Job Queue" using RDBMS and Redis
date:   2020-05-18 13:19:16 +0530
categories: email
excerpt: In this article, we will look at ways to implement the "Job Queue" pattern, using a RDBMS (MySQL) and Redis,  and look at the "Pros" and "Cons" of both the methods. 
permalink: /job-queue.html
comments_id: 5
---
[Introduction](#introduction)

[RDBMS Method 1 - Using "status" column for locking](#rdbms-method-1---using-status-column-for-locking)

[RDBMS Method 2 - Using "SKIP LOCKED"](#rdbms-method-2---using-skip-locked)

[Using Redis as a queue](#using-redis-as-a-queue)

[RDBMS vs Redis. Which one to choose for a job queue?](#rdbms-vs-redis-which-one-to-choose-for-a-job-queue)

# Introduction

"Job Queue" is a common design pattern that would make its way into many applications. In this pattern, we would have a component, let’s say "Receiver" which would receive a request to perform a  particular "Job" from the user, add the job to a "Queue" and return an acknowledgement to the user. And, there would be another component , let’s say "Processor" which would dequeue the jobs from the queue one by one, process it and send back the result to the user or update the result in some data store. To increase throughput and availability, we can have multiple instances of the "Processor" component, running multiple threads, with each thread selecting a job from the queue and processing it concurrently.


![Queue Processing - Architecture](https://paper-attachments.dropbox.com/s_DDAFA35FA0A0A929AE767B5DC6240B5D2933EB3EACE7884F3F55C9C1DEC96C3D_1587196023316_QueueProcessing.png)



> ***Why can’t the "Receiver" process the job and handle the result at the time of receiving the request itself?***  

If the work to be done is not dependent on any other external component and if it is a less time consuming task, then probably we could  perform the job at the time of receiving the request itself. Otherwise it is better to queue the request, process the request and handle the result asynchronously offline.  This helps us in the following ways,


- If it is a time consuming work, asking the user to wait while we are processing the request would not be a good user-experience. Instead we could provide an acknowledgement to the user like a "Request Id" and ask the user to check back later.  Also, if we perform time consuming tasks inline, we are bringing down the concurrency of our server , as threads are busy performing a time consuming task and other user’s  also will start having connectivity or slowness issues.


- If we are dependent on external system to process a request,  then processing it inline means, we are closely coupling our application with the external system.  If the external system is down or experiencing issues, we would have to show an error message to the user. The user would have to again retry the request later.  If using the queuing pattern, we can implement some sort of retry mechanism in the  "Processor" component to handle such situations.  

In this article, we will look at ways to implement this "Job Queue" pattern, using a RDBMS (MySQL) and Redis,  and look at the "Pros" and "Cons" of both the methods.  

Let us consider the following use case,  *"Customer’s of a bank,  log in to the online banking system and submit requests for downloading historical transaction statement. They transaction statement is then sent to the customer via an email message"*

We are mainly interested in looking at, how to have multiple instances of "Processor" components process jobs from the queue concurrently without duplication i.e we should not process the same job twice.  For the methods that are discussed here, I have provided a sample implementation and you can find the source code [here.](https://github.com/messagetobala/messagetobala.github.io/tree/master/Queue-Processor) The implementation is in Java and uses Spring framework.


# Using RDBMS

Let us first try our hand in implementing "Job Queue" using only a RDBMS. In this method, Receiver component accepts the job request from the user, creates an entry in a table called "historical_statement_job" and returns an "id" for the job as an acknowledgement to the user. 

The schema for the table is as below.  
   
``` sql
CREATE TABLE `historical_statement_job` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `customer_id` int unsigned NOT NULL,
  `from_date` date NOT NULL,
  `to_date` date NOT NULL,
  `status` enum('submitted','in_progress','completed','failed') DEFAULT (_utf8mb4'submitted'),
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
```

The "status" column can be one of 

- submitted  -  Request is received from the user.  It has not been selected by any Processor thread for processing.
- in_progress -  The request is being processed by one of the Processor threads.
- completed - The request has been processed successfully.
- failed -  The request was processed but encountered an error during processing.

      
As the jobs are added to the table,  the Processor component should pick up the jobs and process it.  There will be multiple instances of the Processor component and within each instance there would be multiple worker threads, with each thread processing a job. 

> ***How could we make sure that no two Processor instances are processing the same job?***


## RDBMS Method 1 - Using "status" column for locking

One straight forward way is to use the "status" column. Each Processor instance before beginning  to process a job, will try to update the "status" column  from "submitted" to "in_progress".  If it updated the row, the job can be processed by that instance. If it did not update the row, the job should be skipped.  In this method we are using the value of the "status" column as a locking mechanism.

The algorithm for this method  looks something like this,


    Call stored procedure "get_job_list_to_process" to get a batch of jobs that 
    is in "submitted" status.
    For each job
        Call procedure "update_job_status" to update the status of job from 
        "submitted" to "in progress". The procedure should update the status only if the 
        current status is "submitted" and should return if it updated the row or not.

        If the procedure "update_job_status" updated the row, we have acquired the lock.
         Start a new thread and process the job in a separate thread. 
        Else
         Skip this job and move to next job.

Let us take a look at the two stored procedures mentioned in the algorithm.

***get_job_list_to_process***

{% highlight sql linenos %}
create procedure get_job_list_to_process
(
  batch_size int
)
begin
  DECLARE validation_error CONDITION FOR SQLSTATE '45000'; 
  if batch_size is null or batch_size <= 0 then
      signal validation_error set message_text = 'Invalid value for batch_size';     
  end if; 
  -- update status of  any job which is 'in_progress' state for 
  -- 5 minutes or more
  update historical_statement_job set status = 'submitted'
  where status = 'in_progress' and
  timestampdiff(minute, updated_at, now()) >=5;

  select id, customer_id, from_date, to_date, status from  historical_statement_job
  where status = 'submitted'
  order by id asc
  limit batch_size; 
end;
{% endhighlight %}

The stored procedure  accepts a  single argument "batch_size", which is the number of jobs to return.  This allows a Processor instance to handle jobs in batches.    

At line 11, we have a statement that updates the status of the job from "in_progress" to "submitted" if the job was not updated for more than 5 minutes. ***Why do we need this?*** 

Let us say one of the Processor instance crashed. What happens to all the jobs  it was processing when it crashed? All these jobs will have the status column as "in_progress" and will not be picked by any other "Processor" instances.  To protect against such failures, in "get_job_list_to_process" we are updating the status back to "submitted" from "in_progress" for any jobs that has not been updated for more than five minutes.

***update_job_status***

{% highlight sql linenos %}
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
end;
 {% endhighlight %}


The stored procedure accepts the job id , the existing status of the job and new status of the job as input arguments.  It uses output arguments to pass on the number of rows it updated.

Now let us look at the implementation of  the Processor component that will use the above two stored procedures .  The two important classes are "RDBMSHistoricalStatementProcessor_v1" and "HistoricalStatementWorker_v1". I have reproduced the code below for easy reference. 

***RDBMSHistoricalStatementProcessor_v1.java***

{% highlight java linenos %}
public class RDBMSHistoricalStatementProcessor_v1 {

private static final Logger logger = LoggerFactory.getLogger(RDBMSHistoricalStatementProcessor_v1.class);
private static final int BATCH_SIZE = 50;
@Autowired
private HistoricalStatementDAO dao;
@Autowired
private HistoricalStatementWorker_v1 worker;
private ExecutorService executor =  Executors.newFixedThreadPool(BATCH_SIZE);
private  boolean running = true;
public void processHistoricalStatementJobs() {
  while(running) {
    try {
      logger.info("Fetching historical statement jobs to process ");
      List<HistoricalStatementJob> jobs = dao.getJobListToProcess(BATCH_SIZE);
      for (HistoricalStatementJob job : jobs) {
        if (!running) {
          break;
        }
      if (acquireLock(job)) {
        //Lock acquired
        executor.submit(new Runnable() {
          @Override
          public void run() {
            try {
              worker.processJob(job);
            } catch (HistoricalStatementException e) {
              logger.error("Error processing job {}",job.getJobId(), e);
            }
          }
          });
      }
    }
    if (jobs.isEmpty()) {
      logger.info("No pending jobs found for processing. Sleeping for a second");
      try {
    Thread.sleep(1000);
      } catch (InterruptedException e) {
    break;
      }
    }
  } catch(HistoricalStatementException e) {
      logger.error("Error processing jobs",e);
  }
  }
  executor.shutdown();
  try {
    executor.awaitTermination(60, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        logger.error("Interrupted while awaiting termination of thereads.", e);
    }
}

private boolean acquireLock(HistoricalStatementJob job) {
  try {
    if (dao.updateJobStatus(job.getJobId(), 
        JobStatus.SUBMITTED, JobStatus.IN_PROGRESS) == 1) {
      return true;
    }
  } catch (HistoricalStatementException e) {
    logger.error("Error trying to acquire lock for job id {}", job.getJobId(), e);
  }
  return false;
}

public void setRunning(boolean running) {
    this.running = running;
}
}
{% endhighlight %}
The method "processHistoricalStatementJobs" is where our queue processing is implemented. At line 15, we call  the method "getJobListToProcess" which executes the stored procedure "get_job_list_to_process" and returns a list of jobs that are in "submitted" state and needs to be processed.

Then for each job we try to acquire a lock.  This is done in the "acquireLock" method. We call the stored procedure "update_job_status" and try to update the state of the job from "submitted" to "in_progress". If the stored procedure updated the status it would return "1" via the output parameter, which is interpreted as "*the lock has been acquired*".   Otherwise it returns "0", which is interpreted as "*the lock has not been acquired*".

It is possible that two processor instances are trying to update the status of the same job by calling the "update_job_status" stored procedure at the same time with the same job id. In this case only one of the calls to the stored procedure will return "1" and other will return "0".  The Processor instance which gets a return value of 1 will go ahead and process the job.
The other Processor instance which gets a return value of "0" will skip that particular job and move on to the next job.

If the call to  "get_job_list_to_process" did not return any jobs, we  wait for a second and then again poll the database for any new jobs.

After acquiring the lock on a job, we start a new thread and process the job in a separate thread(at line 26). This allows a processor instances to process multiple jobs simultaneously and improve concurrency.

The actual processing of the job is done in "HistoricalStatementWorker_v1" class in the "processJob" method. This is where we implement the business logic of the job. Once the job is done we update the status of the job to "COMPLETED". If we encounter any errors while processing the job we update the status of the job as "FAILED".

***HistoricalStatementWorker_v1.java***

{% highlight java linenos %}
public class HistoricalStatementWorker_v1 {
  private static final Logger logger = LoggerFactory.getLogger(HistoricalStatementWorker_v1.class);
  private HistoricalStatementDAO dao;
  @Autowired
  public HistoricalStatementWorker_v1(HistoricalStatementDAO dao) {
    this.dao = dao;
  }
  private void doWork(HistoricalStatementJob job) throws HistoricalStatementException   {
    //Do actual work
  }
  public void processJob(HistoricalStatementJob job) throws HistoricalStatementException {  
      logger.info("Processing job {}", job.getJobId());     
      try {
        doWork(job);
        //Update status to completed.
        dao.updateJobStatus(job.getJobId(), JobStatus.IN_PROGRESS, 
        JobStatus.COMPLETED);
      } catch(HistoricalStatementException e) {
        if (e.isPermFailure()) {
          dao.updateJobStatus(job.getJobId(), JobStatus.IN_PROGRESS, 
          JobStatus.FAILED);
        }
        //otherwise will be retried.
      }  
  }
}
{% endhighlight %}      

## RDBMS Method 2 - Using "SKIP LOCKED" ##

One drawback of  "Method 1" is that though we are processing the job concurrently across multiple Processor instances, it is not strictly 100% concurrent.  This is because, each Processor instance still has to look at every job and try to acquire a lock.  It will be good if each Processor instance can get distinct jobs from the database and process it without have to acquire lock. 

Assuming that in the job table we have the following two jobs in "submitted" status,

```
+----+-------------+------------+------------+-----------+---------------------+---------------------+
| id   | customer_id | from_date  | to_date   | status   | updated_at    | created_at  |
+----+-------------+------------+------------+-----------+---------------------+---------------------+
|  1   |     1   | 2019-05-01 | 2020-05-01 | completed | 2020-05-17 11:30:14 | 2020-05-17 11:21:20 |
|  2   |     2   | 2019-10-01 | 2020-05-01 | completed | 2020-05-17 11:30:19 | 2020-05-17 11:21:41 |
+----+-------------+------------+------------+-----------+---------------------+---------------------+
```

What happens if two Processor instances executes the following query ?

``` sql
select * from historical_statement_job where status order by id limit 1; 
```
As you  might have guessed, both the Processor instances would get the same job record.  

time  | Transaction  in  1st Processor instance | Transaction in 2nd Processor instance            
----- | --------------------------------------- | --------------------- 
t1    | start transaction;           | start transaction;     
t2    |#>select * from historical_statement_job where status = 'submitted' order by id limit 1; <br> _____ <br> id:1 (Showing only 'id' column due to space constranints)| #>select * from historical_statement_job where status = 'submitted' order by id limit 1; <br> _____ <br> id:1 (Showing only 'id' column due to space constranints)
t3    | commit; | commit;


This is because both the instances are issuing a select query, which results in a read lock on the same row and that row is returned to both the instances.

Let us try modifying the select query to include a "FOR UPDATE" clause. By adding this, we are indicating to the database that the selected row is going to be updated. This will cause the DBMS to obtain a write lock on the row instead of read lock. If the two Processor instances are issuing "SELECT FOR UPDATE" query at the same time, only one of the query would be successful in getting the write lock on the row, and it will return that row. But what happens to the query from the second instance which did not get write lock.

time  | Transaction  in  1st Processor instance | Transaction in 2nd Processor instance            
----- | --------------------------------------- | --------------------- 
t1    | start transaction;           | start transaction;     
t2    |#>select * from historical_statement_job where status = 'submitted' order by id limit 1 for update; <br> _____ <br> id:1 | #>select * from historical_statement_job where status = 'submitted' order by id limit 1 for update; <br> _____ <br> ERROR 1205 (HY000): Lock wait timeout exceeded; try restarting transaction
t3    | commit; | commit;                                                                                                                                                                              |

As you can see from the above sample output, one query was successful in retrieving a single row. But the second query timed out waiting to obtain write lock.  Ideally, for the second query, we want the database to skip the row with a write lock and return the next row that matches the select criteria.  This is achieved through the "SKIP LOCKED" option.  Let us modify our select query to include this option as well and repeat of experiment.


| time | Transaction  in  1st Processor instance                                                                                                               | Transaction in 2nd Processor Instance                                                                                                           |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| t1   | start transaction;                                                                                                                                    | start transaction;                                                                                                                              |
| t2   | #>select * from historical_statement_job where status = 'submitted' order by id limit 1 for update skip locked; <br> _____ <br> id:2| #>select * from historical_statement_job where status = 'submitted' order by id limit 1 for update skip locked; <br> _____ <br> id:2
| t3   | update historical_statement_job set status = 'in_progress' where id = 1                                                                               | update historical_statement_job set status = 'in_progress' where id = 2                                                                         |
| t4   | update historical_statement_job set status = 'completed' where id = 1;                                                                                | update historical_statement_job set status = 'completed' where id = 2;                                                                          |
| t5   | commit;                                                                                                                                               | commit;                                                                                                                                         |


As you can see , now both the Processor instance get a unique job record, which they can process concurrently.  

One important point to remember is that the above will work only if the transaction isolation is "Repeatable Read" or "Serializable".

The implementation of this approach is in the classes "RDBMSHistoricalStatementProcessor_v2" and "HistoricalStatementWorker_v2".

***RDBMSHistoricalStatementProcessor_v2.java***

{% highlight java linenos %}
public class RDBMSHistoricalStatementProcessor_v2 {

  private static final Logger logger = LoggerFactory.getLogger(RDBMSHistoricalStatementProcessor_v2.class);
  private static final int BATCH_SIZE = 50;
  @Autowired
  private HistoricalStatementWorker_v2 worker;  
  private  boolean running = true;
  private ExecutorService executor =  Executors.newFixedThreadPool(BATCH_SIZE);
 
  public void processHistoricalStatementJobs() {   
    for (int i = 0; i < BATCH_SIZE; i++) {
      logger.info("Starting thread {} to process historical statement jobs" , i+1);
      executor.submit(new Runnable() {
        @Override
          public void run() {
            while (running) {
              try {
                HistoricalStatementJob job = worker.processJob();
                if (job == null) {
                  try {
                    logger.info("No pending jobs found for processing. Sleeping for a second");
                    Thread.sleep(1000);
                  } catch (InterruptedException e) {
                    break;
                  }
                }  
              } catch (HistoricalStatementException e) {
                logger.error("Error processing job", e);
              }
            }
          }
      });
    }
  }
  
  public void setRunning(boolean running) {
    this.running = running;
  }
}
{% endhighlight %} 

In "processHistoricalStatementJobs" method we use the ExecutorService to start multiple job processor threads.  Each job processor thread calls "processJob" method in class "HistoricalStatementWorker_v2" , which would get a single job from the database, process it , update the result and return the job information..   If "processJob" did not find any job in the database, the job processor thread would wait for a second before polling the database again.

{% highlight java linenos %}
public class HistoricalStatementWorker_v2 {
  private static final Logger logger = LoggerFactory.getLogger(HistoricalStatementWorker_v2.class);   
  private HistoricalStatementDAO dao;
  @Autowired
  public HistoricalStatementWorker_v2(HistoricalStatementDAO dao) {
      this.dao = dao;
  }
    private void doWork(HistoricalStatementJob job) throws HistoricalStatementException    {
      //Do actual work
    }  

    @Transactional(rollbackFor = HistoricalStatementException.class)
    public HistoricalStatementJob processJob() throws HistoricalStatementException {
      HistoricalStatementJob job = dao.getJobToProcess();
      if (job != null) {
      try {
        doWork(job);
        //Update status to completed.
        dao.updateJobStatus(job.getJobId(), 
            JobStatus.IN_PROGRESS, JobStatus.COMPLETED);
      } catch(HistoricalStatementException e) {
        if (e.isPermFailure()) {
          dao.updateJobStatus(job.getJobId(), 
              JobStatus.IN_PROGRESS, JobStatus.FAILED);
        }
        //otherwise will be retried.
      }  
    }
    return job;
  }
}
{% endhighlight %}   

One important thing is, if you look at "processJob" method you will notice that it is annotated with the Spring "@Transactional" annotation. This ensures that all the database operations like selecting the job row , the actual processing of the job and updating the result happens in a single transaction. 

This ensures that if any errors occurs during the processing of the job, the transaction is rolled back. So the write lock on the corresponding job row goes away and another instance can try processing the job again.

The "SKIP LOCKED" clause is available from MySQL 8.0.1 version. It is also available in Postgres and SQL Server databases.

## Using Redis as a queue

Redis is a in-memory data store which provides a set of data types(Set, Lists etc) and pre-defined list of operations that can be performed on each of these data types. Though widely used for implementing a caching layer, it could also be used as a messaging queue, to implement distributed locking and as pub/sub messaging system.

To implement our "Job Queue" using Redis, we could make use of its "Lists" data type  A Redis list data type allows us to store one or more elements in a key. We could add elements either at the head of the list (Using LPUSH operation) or at the tail of the list (Using RPUSH). Similarly we could pop elements from the list either at the tail position(RPOP) or at the head position(LPOP).

While using Redis, the Receiver component, as it receives new job requests, would add the jobs to a Redis List using the LPUSH operation. The Processor instances could then pop these jobs using the RPOP operation and process them. Redis ensures that if there two RPOP operations at the same instant, the same element is not popped twice. So, we need not worry about the same job being processed by two Processor instances.

Our sample implementation of the Processor component using Redis is in the classes "RedisHistoricalStatementProcessor" and "RedisHistoricalStatementWorker".

***RedisHistoricalStatementProcessor.java*** 

{% highlight java linenos %}
public class RedisHistoricalStatementProcessor {
private static final Logger logger = LoggerFactory.getLogger(RedisHistoricalStatementProcessor.class);
private static final int BATCH_SIZE = 50;
@Autowired
private RedisHistoricalStatementWorker worker;  
private ExecutorService executor =  Executors.newFixedThreadPool(BATCH_SIZE);
public void processHistoricalStatementJobs() {
  for (int i = 0; i < 50; i++) {
    logger.info("Starting thread {} to process historical statement jobs" , i+1);
      executor.submit(new Runnable() {
        @Override
        public void run() {
          worker.processJobs();
        }
    });
  }
  }
}
{% endhighlight %}

In "processHistoricalStatementJobs" method of the class "RedisHistoricalStatementProcessor" we start multiple threads using the ExecutorService interface. Each thread calls the "processJobs" method in class  "RedisHistoricalStatementWorker" where the actual processing of the jobs happen.

***RedisHistoricalStatementWorker.java***

{% highlight java linenos %}
public class RedisHistoricalStatementWorker {
  private static final Logger logger = LoggerFactory.getLogger(RedisHistoricalStatementWorker.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  @Autowired
  @Qualifier("redisHost")  
  private String redisHost;
  @Autowired
  @Qualifier("redisPort")  
  private String redisPort;
  @Autowired
  private HistoricalStatementDAO dao;
  private  boolean running = true;
  public void processJobs() {    
    Jedis jedis = new Jedis(redisHost, Integer.parseInt(redisPort));
    while (running) {
      String jobStr = jedis.brpoplpush("pending_jobs", "in_progress_jobs", 5000);
      if (jobStr != null) {
        HistoricalStatementJob job = null;  
        try {
          job = (HistoricalStatementJob) mapper.readValue(jobStr, HistoricalStatementJob.class);
          Long jobId = Long.parseLong(jobStr);
          processJob(jobId);
          jedis.lrem("in_progress_jobs", 1, jobStr);
          } catch (HistoricalStatementException e){
              if (e.isPermFailure()) {
                try {
                  dao.updateJobStatus(job.getJobId(), 
                        JobStatus.IN_PROGRESS, JobStatus.FAILED);
                } catch (HistoricalStatementException t) {
                    logger.error("Error processing job.", t.getMessage());
              }
        }
        } catch (Exception e) {
          logger.error("Error processing job {}", jobStr, e);
        }  
      } else {
        logger.info("No jobs found to process");
      }  
    }
    jedis.close();
  }

  public void setRunning(boolean running) {
    this.running = running;
  }
  
  private void processJob(long jobId) throws HistoricalStatementException {
    logger.info("Processing job {}", jobId);
    //Do actual work    
  }    
}
{% endhighlight %}

In  "processJobs" method we have big while loop , where we pop jobs from a list called "pending_jobs" and process them. Once a job is processed, we will pop the next job and process it.

Notice that in line 16, we are using the BRPOPLPUSH operation to pop an element from the list and not RPOP.  It is similar to RPOP , but it will block for the specified duration if there are elements to pop from the list.  Another functionality provided by this operation is that in addition to popping and returning an element from a list, it will also push that element into another list . **Both the pop and push operations happens in a atomic manner.**  In the sample implementation we are popping elements from the list  "pending_jobs" and pushing them into the list "in_progress_jobs". After a job is processed successfully, we are removing it from the list "in_progress_jobs".

**So, why do we need to push elements into another list?** This is required to properly handle failure scenarios.  For example, lets say the Processor instance crashed while it was processing a job.
Now if the job information was not added to a second list, then the job is lost. We cannot finish processing the job. But if the job information was added to a second list, we can implement some sort of error handling mechanism, by going through this second list.


## RDBMS vs Redis. Which one to choose for a job queue?

What are the pros and cons of choosing either RDBMS or Redis for implementing a job queue ?
Most of the applications would definitely have an RDBMS as part of its existing architecture. So using RDBMS for job queue means we are not introducing a new component. Also, even if use Redis for job queue, we might still have to store information about the jobs in a RDBMS for features like reporting.

Another advantage, I see  in  using RDBMS is that, we need not always select a job for processing on a first come first serve basis. For example, in the sample implementation, we are selecting jobs ordered by the id column. Instead, we could select jobs by other criteria, like processing the jobs of customers who are in paid plan before processing the jobs of free customers. We could implement any business logic in the database while selecting a job for processing.  If using Redis, this might not be possible or might not be straight forward.  For example, if we want to differentiate jobs of paid customers and free customers, we might have to use two different lists.

But the disadvantage in RDBMS is that, the locking mechanism of RDBMS could add a performance overhead. Due to this, we could be dequeuing jobs  at a slower rate than it is enqueued.  This could be OK in certain scenarios. For example, we might have period of inactivity where no new jobs are submitted and we would eventually process all the pending jobs. 

Also with a RDBMS implementation, the processor instances are polling the database at configured intervals. This could place additional load on the database if there are many processor instances. In Redis on the other hand, we have a blocking mechanism.

In my opinion, if a system already has a RDBMS and we want to introduce a job queue, we should definitely first look, if we can use RDBMS for implementing our job queue. If using a RDBMS results in performance bottle-neck, we could  look at Redis.

