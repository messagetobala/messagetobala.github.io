package org.bala.historicalstatement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ComponentFactory {
	
	@Autowired
	@Qualifier("rdbmsHost")
	private String rdbmsHost;
	
	@Autowired	
	@Qualifier("rdbmsPort")
	private String rdbmsPort;
	
	@Autowired
	@Qualifier("rdbmsUser")	
	private String rdbmsUser;
	
	@Autowired
	@Qualifier("rdbmsPassword")	
	private String rdbmsPassword;
	
	
	public  DataSource getDataSource() {
		
		HikariConfig hConfig = new HikariConfig();
		hConfig.setJdbcUrl("jdbc:mysql://" + rdbmsHost + ":" + rdbmsPort + "/queue_processing?allowPublicKeyRetrieval=true&useSSL=false");
		hConfig.setUsername(rdbmsUser);
		hConfig.setPassword(rdbmsPassword);
		hConfig.setMaximumPoolSize(50);
		hConfig.setConnectionTestQuery("SELECT 1");
		
		return new HikariDataSource(hConfig);
	}
	

}
