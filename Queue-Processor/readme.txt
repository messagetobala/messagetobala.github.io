Update values for redis & mysql details like host, port and host informationn in application.xml
RdbmsReceiver.java - To load sample data into Mysql for the RDBMS based solution.
RedisReceiver.java - To load sample data into Redis for the Redis based solution. 

RDBMSHistoricalStatementProcessor_v1.java - RDBMS implementation using "status" column for locking.
RDBMSHistoricalStatementProcessor_v2.java - RDMS implementation using "skip locked"

RedisHistoricalStatementProcessor.java - Redis implementation.
