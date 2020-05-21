/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class that provides static methods for obtaining JDBC Connections from
 * a {@link DataSource}. Includes special support for Spring-managed
 * transactional Connections, e.g. managed by {@link DataSourceTransactionManager}
 * or {@link org.springframework.transaction.jta.JtaTransactionManager}.
 *
 * <p>Used internally by Spring's {@link org.springframework.jdbc.core.JdbcTemplate},
 * Spring's JDBC operation objects and the JDBC {@link DataSourceTransactionManager}.
 * Can also be used directly in application code.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConnection
 * @see #releaseConnection
 * @see DataSourceTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public abstract class DataSourceUtils {

	/**
	 * Order value for TransactionSynchronization objects that clean up JDBC Connections.
	 */
	public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;

	private static final Log logger = LogFactory.getLog(DataSourceUtils.class);


	/**
	 * Obtain a Connection from the given DataSource. Translates SQLExceptions into
	 * the Spring hierarchy of unchecked generic data access exceptions, simplifying
	 * calling code and making any exception that is thrown more meaningful.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using {@link DataSourceTransactionManager}. Will bind a Connection to the
	 * thread if transaction synchronization is active, e.g. when running within a
	 * {@link org.springframework.transaction.jta.JtaTransactionManager JTA} transaction).
	 * @param dataSource the DataSource to obtain Connections from
	 * @return a JDBC Connection from the given DataSource
	 * @throws org.springframework.jdbc.CannotGetJdbcConnectionException
	 * if the attempt to get a Connection failed
	 * @see #releaseConnection
	 */
	public static Connection getConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
		try {
			return doGetConnection(dataSource);
		}
		catch (SQLException ex) {
			throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
		}
	}

	/**
	 * Actually obtain a JDBC Connection from the given DataSource.
	 * Same as {@link #getConnection}, but throwing the original SQLException.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using {@link DataSourceTransactionManager}. Will bind a Connection to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
	 * @param dataSource the DataSource to obtain Connections from
	 * @return a JDBC Connection from the given DataSource
	 * @throws SQLException if thrown by JDBC methods
	 * @see #doReleaseConnection
	 * JDBC （java Data Base Connectivity ,java 数据库连接） 是一种用于执行 SQL 语句的 Java API ，可以为多种关系数据库提供了
	 * 统一的访问，它由一组用 java 语言编写的类和接口组成，JDBC 为数据库开发人员提供了一个标准的 API ,据此可以构建更高级的工具和接口，
	 * 使数据库开发人员能够用纯 java API 编写数据库应用程序，并且可跨平台运行，并且不受数据库供应商限制，
	 * (1) 在开发环境中加载指定的数据库驱动程序，接下来的实验中，使用数据库是 MYSQL ,所以要去下载 JDBC 驱动程序
	 * (2) 在 java 程序中加载驱动程序，在 Java 程序中，可以通过"Class.forName(" 指定数据库的驱动程序")"的方式来加载添加到开发环境中
	 * 中的驱动程序，例如加载 MySQL 的数据库驱动程序的代码为 Class.forName("com.mysql.jdbc.Driver");
	 * (3) 创建数据库连接对象，通过 DriverManager 类创建数据库连接对象 Connnection ，DriverManager 类作用于 Java 程序和 JDBC 驱动
	 * 驱动程序之间，用于检查所加载的驱动程序是否以建立连接，然后通过它 getConnection 方法根据数据库 URL ，用户名和密码，创建一个 JDBC
	 * Connection 对象，例如 Connection connectonMySql = DriverManager.getConnection("数据库的 URL ","用户名"," 密码") ，其中 URL =
	 * 协义名+IP 地址 +  端口 +  数据库名称;用户名和密码是指登录数据库时所使用的用户名和密码，具体的示例创建 MySql 的数据库连接代码如下：
	 * Connection connectionMySQL = DriverManager.getConnection("jdbc:mysql://localhost:3306/myuser","root","123456");
	 * (4) 创建Statement 对象，Statement 类主要用于执行静态的 SQL 语句并返回它所生成的结果对象，通过 Connection 对象的 createStatement()
	 * 方法可以创建一个 Statement 对象，例如 Statement statement = connection.createConnection() ，具体的示例如下：
	 * 	Statement statementMysql = connectonMySql.createStatement();
	 * (5) 调用 Statement 对象相关的方法执行相对应的 SQL 语句，通过 execuUpdate()方法来对数据更新，包括插入的和删除操作，例如
	 *  向 staff 表中插入一条数据的代码
	 *  statement.excuteUpdate("INSERT INTO staff(name,age,sex,address,depart,worklen,wage) VALUES ('tom1',321,'M','china','Personnel','3','3000')");
	 *  通过调用 Statement 对象的 executeQuery()方法进行数据的查询，而查询的结果会得到 ResultSet 对象，ResultSet 表示执行查询数据库后
	 *  返回的数据的集合，ResultSet 对象具有可以指向当前数据行的指针，通过该对象的 next() 方法，使得指针下一行，然后将数据以列号或者字段
	 *  名取出，如果当 next() 方法返回了 null,则表示下一行中没有数据存在，使用示例代码如下：
	 *  ResultSet resultSel = statement.executeQuery("select * from staff");
	 *  (6) 关闭数据库连接，使用完数据库或者不需要访问数据库时，通过 Connection 的 close()方法及相关的关闭数据库连接
	 *
	 *  8.1 Spring  连接数据库程序实现
	 *  Spring 中的 JDBC 连接与直接使用 JDBC 去连接还是有所差别的，Spring 对 JDBC 做了大量的封装，消除了冗余的代码，使得开发量大大的
	 *  减少了，下面通过一个小例子让大家简单的认识一下 Spring 中的 JDBC 操作
	 *  (1)创建数据库
	 *  create table user {
	 *      id int (11) not null auto_increatement,
	 *      name varchar(255) default null,
	 *      age int default null ,
	 *      sex varchar(255) default null,
	 *      primary key (id)
	 *  }engine = innodb default charset=utf=8 ;
	 *
	 *  (2) 创建对应的数据库表 PO
	 *  public class User {
	 *      private int id ;
	 *      private String name;
	 *      private int age ;
	 *      private String sex;
	 *  }
	 *  (3) 创建表与实体间的映射
	 *  public class UserRowMapper implements RowMapper{
	 * @Override
	 * 		public Object mapRow(ResultSet set,int index) throws SQLException {
	 * 		 	User person = new User(
	 * 		 	set.getInt("id"),
	 * 		 	set.getString("name"),
	 * 		 	set.getInt("age"),
	 * 		 	set.getString("sex")
	 * 		 );
	 * 		}
	 *  }
	 *  (4) 创建数据库操作接口
	 *  public interface UserService{
	 *      public void save(User user);
	 *
	 *      public List<User> getUsers();
	 *  }
	 *
	 *  (5) 创建数据库操作接口实现类
	 *
	 *	public class UserServiceImpl implements UserService {
	 *	   private JdbcTemplate jdbcTemplate ;
	 *
	 *	   // 设置数据源
	 *	   public void setDatasource(DataSource dataSource){
	 *	       this.jdbcTemplate = new JdbcTemplate(dataSource);
	 *	   }
	 *
	 *	   public void save(User user ){
	 *	       jdbcTemplate.update("insert into user (name,age,sex) values (?,?,?)",new Object []
	 *	       {
	 *	           user.getName(),user.getAge(),
	 *	           user.getSex()
	 *	       },
	 *	       new int[] {java.sql.Types.VARCHAR,java.sql.Types.INTEGER,java.sql.Types.VARCHAR}
	 *	       );
	 *	   }
	 *
	 * @SuppreWarnings("unchecked")
	 * 		public List<User> getUsers(){
	 * 			List<User> list = jdbcTemplate.query("select * from user",new UserRowMapper());
	 * 		return list ;
	 * 	}
	 *	}
	 *
	 * (6)  创建 spring  配置文件
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
	 *
	 *            <!--配置数据源-->
	 *            <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
	 *            		<property name="DriverClassName" value="com.mysql.jdbc.Driver"></property>
	 *            		<property name="url" value="jdbc:mysql://locahost:3306/test"></proprty>
	 *            		<property name="username" value="root"></property>
	 *            		<property name="password" value="123456"></property>
	 *            	 	<!--连接池启动时的初始值-->
	 *            		<property name="initialSize" value ="1"></property>
	 *            		<!--连接池最大值-->
	 *            		<property name="maxActive" value ="300"></property>
	 *            		<!-- 最大空闲值，当经过一个高峰时间后，连接池可以慢慢的将已经用不到的连接慢慢的释放一部分，一直减少到 maxIdle 为止-->
	 *            		<property name="maxIdle" value ="2"></property>
	 *            		<!-- 最小空闲值，当空闲的连接数少于阀值时，连接池就会增申请一些连接，以免洪峰来不及申请-->
	 *            		<property name="minIdle" value ="1"></property>
	 *            		<property name="" value =""></property>
	 *            		<property name="" value =""></property>
	 *            		<property name="" value =""></property>
	 *            		<property name="" value =""></property>
	 *            </bean>
	 *            <!--配置业务 bean : PersonServiceBean-->
	 *			<bean id="userService" class="service.UserServiceImpl">
	 *			 	<!--向属性 dataSource 注入数据源-->
	 *			 	<property name="dataSource" ref="dataSource"></property>
	 *			 </bean>
	 *
	 * (7)测试
	 * 	public class SpringJDBCTest{
	 * 	    public static void main(String [] args ){
	 * 	        ApplicationContext act = new ClassPathXmlApplicationContext("bean.xml");
	 * 	        UserService userSerivce = (UserService)act.getBean("userService");
	 * 	        User user = new User();
	 * 	        user.setName("张三");
	 * 	        user.setAge(20);
	 * 	        user.setSex("男");
	 * 	        userService.save(user);
	 * 	        List<User>  person1 = userService.getUsers();
	 * 	        for(User person2 : person1 ){
	 * 	        	System.out.println(person2.getId() + " " + person2.getName() + " "+ person2.getAge() + " "+person2.getSext());
	 * 	        }
	 *
	 * 	    }
	 * 	}
	 *
	 *
	 *
	 *
	 *
	 * 	|
	 *
	 * 	1. 获取数据库连接
	 * 	获取数据库连接也并非直接使用 dataSource.getConnection()方法那样的简单，同样也考虑了诸多的情况
	 *
	 *  在数据库连接方面，Spring 主要考虑的是关于事务方面的处理，基于事务的处理的特殊性，Spring 需要保证线程中的数据库操作都是使用同
	 *  一个事务连接
	 */
	public static Connection doGetConnection(DataSource dataSource) throws SQLException {
		Assert.notNull(dataSource, "No DataSource specified");

		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
			conHolder.requested();
			if (!conHolder.hasConnection()) {
				logger.debug("Fetching resumed JDBC Connection from DataSource");
				conHolder.setConnection(dataSource.getConnection());
			}
			return conHolder.getConnection();
		}
		// Else we either got no holder or an empty thread-bound holder here.

		logger.debug("Fetching JDBC Connection from DataSource");
		Connection con = dataSource.getConnection();
		//  当前线程支持同步
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for JDBC Connection");
			// Use same Connection for further JDBC actions within the transaction.
			// Thread-bound object will get removed by synchronization at transaction completion.
			// 在事务中使用同一数据库连接
			ConnectionHolder holderToUse = conHolder;
			if (holderToUse == null) {
				holderToUse = new ConnectionHolder(con);
			}
			else {
				holderToUse.setConnection(con);
			}
			// 记录数据库连接
			holderToUse.requested();
			TransactionSynchronizationManager.registerSynchronization(
					new ConnectionSynchronization(holderToUse, dataSource));
			holderToUse.setSynchronizedWithTransaction(true);
			if (holderToUse != conHolder) {
				TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
			}
		}

		return con;
	}

	/**
	 * Prepare the given Connection with the given transaction semantics.
	 * @param con the Connection to prepare
	 * @param definition the transaction definition to apply
	 * @return the previous isolation level, if any
	 * @throws SQLException if thrown by JDBC methods
	 * @see #resetConnectionAfterTransaction
	 */
	public static Integer prepareConnectionForTransaction(Connection con, TransactionDefinition definition)
			throws SQLException {

		Assert.notNull(con, "No Connection specified");

		// Set read-only flag.
		// 设置数据连接只读标识
		if (definition != null && definition.isReadOnly()) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting JDBC Connection [" + con + "] read-only");
				}
				con.setReadOnly(true);
			}
			catch (SQLException ex) {
				Throwable exToCheck = ex;
				while (exToCheck != null) {
					if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
						// Assume it's a connection timeout that would otherwise get lost: e.g. from JDBC 4.0
						throw ex;
					}
					exToCheck = exToCheck.getCause();
				}
				// "read-only not supported" SQLException -> ignore, it's just a hint anyway
				logger.debug("Could not set JDBC Connection read-only", ex);
			}
			catch (RuntimeException ex) {
				Throwable exToCheck = ex;
				while (exToCheck != null) {
					if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
						// Assume it's a connection timeout that would otherwise get lost: e.g. from Hibernate
						throw ex;
					}
					exToCheck = exToCheck.getCause();
				}
				// "read-only not supported" UnsupportedOperationException -> ignore, it's just a hint anyway
				logger.debug("Could not set JDBC Connection read-only", ex);
			}
		}

		// Apply specific isolation level, if any.
		// 设置数据库连接隔离级别
		Integer previousIsolationLevel = null;
		if (definition != null && definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			if (logger.isDebugEnabled()) {
				logger.debug("Changing isolation level of JDBC Connection [" + con + "] to " +
						definition.getIsolationLevel());
			}
			int currentIsolation = con.getTransactionIsolation();
			if (currentIsolation != definition.getIsolationLevel()) {
				previousIsolationLevel = currentIsolation;
				con.setTransactionIsolation(definition.getIsolationLevel());
			}
		}

		return previousIsolationLevel;
	}

	/**
	 * Reset the given Connection after a transaction,
	 * regarding read-only flag and isolation level.
	 * @param con the Connection to reset
	 * @param previousIsolationLevel the isolation level to restore, if any
	 * @see #prepareConnectionForTransaction
	 */
	public static void resetConnectionAfterTransaction(Connection con, Integer previousIsolationLevel) {
		Assert.notNull(con, "No Connection specified");
		try {
			// Reset transaction isolation to previous value, if changed for the transaction.
			if (previousIsolationLevel != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Resetting isolation level of JDBC Connection [" +
							con + "] to " + previousIsolationLevel);
				}
				con.setTransactionIsolation(previousIsolationLevel);
			}

			// Reset read-only flag.
			if (con.isReadOnly()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Resetting read-only flag of JDBC Connection [" + con + "]");
				}
				con.setReadOnly(false);
			}
		}
		catch (Throwable ex) {
			logger.debug("Could not reset JDBC Connection after transaction", ex);
		}
	}

	/**
	 * Determine whether the given JDBC Connection is transactional, that is,
	 * bound to the current thread by Spring's transaction facilities.
	 * @param con the Connection to check
	 * @param dataSource the DataSource that the Connection was obtained from
	 * (may be {@code null})
	 * @return whether the Connection is transactional
	 */
	public static boolean isConnectionTransactional(Connection con, DataSource dataSource) {
		if (dataSource == null) {
			return false;
		}
		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		return (conHolder != null && connectionEquals(conHolder, con));
	}

	/**
	 * Apply the current transaction timeout, if any,
	 * to the given JDBC Statement object.
	 * @param stmt the JDBC Statement object
	 * @param dataSource the DataSource that the Connection was obtained from
	 * @throws SQLException if thrown by JDBC methods
	 * @see Statement#setQueryTimeout
	 */
	public static void applyTransactionTimeout(Statement stmt, DataSource dataSource) throws SQLException {
		applyTimeout(stmt, dataSource, -1);
	}

	/**
	 * Apply the specified timeout - overridden by the current transaction timeout,
	 * if any - to the given JDBC Statement object.
	 * @param stmt the JDBC Statement object
	 * @param dataSource the DataSource that the Connection was obtained from
	 * @param timeout the timeout to apply (or 0 for no timeout outside of a transaction)
	 * @throws SQLException if thrown by JDBC methods
	 * @see Statement#setQueryTimeout
	 */
	public static void applyTimeout(Statement stmt, DataSource dataSource, int timeout) throws SQLException {
		Assert.notNull(stmt, "No Statement specified");
		Assert.notNull(dataSource, "No DataSource specified");
		ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		if (holder != null && holder.hasTimeout()) {
			// Remaining transaction timeout overrides specified value.
			stmt.setQueryTimeout(holder.getTimeToLiveInSeconds());
		}
		else if (timeout >= 0) {
			// No current transaction timeout -> apply specified value.
			stmt.setQueryTimeout(timeout);
		}
	}

	/**
	 * Close the given Connection, obtained from the given DataSource,
	 * if it is not managed externally (that is, not bound to the thread).
	 * @param con the Connection to close if necessary
	 * (if this is {@code null}, the call will be ignored)
	 * @param dataSource the DataSource that the Connection was obtained from
	 * (may be {@code null})
	 * @see #getConnection
	 */
	public static void releaseConnection(Connection con, DataSource dataSource) {
		try {
			doReleaseConnection(con, dataSource);
		}
		catch (SQLException ex) {
			logger.debug("Could not close JDBC Connection", ex);
		}
		catch (Throwable ex) {
			logger.debug("Unexpected exception on closing JDBC Connection", ex);
		}
	}

	/**
	 * Actually close the given Connection, obtained from the given DataSource.
	 * Same as {@link #releaseConnection}, but throwing the original SQLException.
	 * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
	 * @param con the Connection to close if necessary
	 * (if this is {@code null}, the call will be ignored)
	 * @param dataSource the DataSource that the Connection was obtained from
	 * (may be {@code null})
	 * @throws SQLException if thrown by JDBC methods
	 * @see #doGetConnection
	 */
	public static void doReleaseConnection(Connection con, DataSource dataSource) throws SQLException {
		if (con == null) {
			return;
		}
		if (dataSource != null) {
			// 当前线程存在事务的情况下说明存在共用的数据库连接直接使用 ConnectionHoler 中的 released 方法进行连接数减一而不是真正的
			//  释放连接
			ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
			if (conHolder != null && connectionEquals(conHolder, con)) {
				// It's the transactional Connection: Don't close it.
				conHolder.released();
				return;
			}
		}
		logger.debug("Returning JDBC Connection to DataSource");
		doCloseConnection(con, dataSource);
	}

	/**
	 * Close the Connection, unless a {@link SmartDataSource} doesn't want us to.
	 * @param con the Connection to close if necessary
	 * @param dataSource the DataSource that the Connection was obtained from
	 * @throws SQLException if thrown by JDBC methods
	 * @see Connection#close()
	 * @see SmartDataSource#shouldClose(Connection)
	 */
	public static void doCloseConnection(Connection con, DataSource dataSource) throws SQLException {
		if (!(dataSource instanceof SmartDataSource) || ((SmartDataSource) dataSource).shouldClose(con)) {
			con.close();
		}
	}

	/**
	 * Determine whether the given two Connections are equal, asking the target
	 * Connection in case of a proxy. Used to detect equality even if the
	 * user passed in a raw target Connection while the held one is a proxy.
	 * @param conHolder the ConnectionHolder for the held Connection (potentially a proxy)
	 * @param passedInCon the Connection passed-in by the user
	 * (potentially a target Connection without proxy)
	 * @return whether the given Connections are equal
	 * @see #getTargetConnection
	 */
	private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
		if (!conHolder.hasConnection()) {
			return false;
		}
		Connection heldCon = conHolder.getConnection();
		// Explicitly check for identity too: for Connection handles that do not implement
		// "equals" properly, such as the ones Commons DBCP exposes).
		return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
				getTargetConnection(heldCon).equals(passedInCon));
	}

	/**
	 * Return the innermost target Connection of the given Connection. If the given
	 * Connection is a proxy, it will be unwrapped until a non-proxy Connection is
	 * found. Otherwise, the passed-in Connection will be returned as-is.
	 * @param con the Connection proxy to unwrap
	 * @return the innermost target Connection, or the passed-in one if no proxy
	 * @see ConnectionProxy#getTargetConnection()
	 */
	public static Connection getTargetConnection(Connection con) {
		Connection conToUse = con;
		while (conToUse instanceof ConnectionProxy) {
			conToUse = ((ConnectionProxy) conToUse).getTargetConnection();
		}
		return conToUse;
	}

	/**
	 * Determine the connection synchronization order to use for the given
	 * DataSource. Decreased for every level of nesting that a DataSource
	 * has, checked through the level of DelegatingDataSource nesting.
	 * @param dataSource the DataSource to check
	 * @return the connection synchronization order to use
	 * @see #CONNECTION_SYNCHRONIZATION_ORDER
	 */
	private static int getConnectionSynchronizationOrder(DataSource dataSource) {
		int order = CONNECTION_SYNCHRONIZATION_ORDER;
		DataSource currDs = dataSource;
		while (currDs instanceof DelegatingDataSource) {
			order--;
			currDs = ((DelegatingDataSource) currDs).getTargetDataSource();
		}
		return order;
	}


	/**
	 * Callback for resource cleanup at the end of a non-native JDBC transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class ConnectionSynchronization extends TransactionSynchronizationAdapter {

		private final ConnectionHolder connectionHolder;

		private final DataSource dataSource;

		private int order;

		private boolean holderActive = true;

		public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
			this.connectionHolder = connectionHolder;
			this.dataSource = dataSource;
			this.order = getConnectionSynchronizationOrder(dataSource);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public void suspend() {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResource(this.dataSource);
				if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
					// Release Connection on suspend if the application doesn't keep
					// a handle to it anymore. We will fetch a fresh Connection if the
					// application accesses the ConnectionHolder again after resume,
					// assuming that it will participate in the same transaction.
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					this.connectionHolder.setConnection(null);
				}
			}
		}

		@Override
		public void resume() {
			if (this.holderActive) {
				TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
			}
		}

		@Override
		public void beforeCompletion() {
			// Release Connection early if the holder is not open anymore
			// (that is, not used by another resource like a Hibernate Session
			// that has its own cleanup via transaction synchronization),
			// to avoid issues with strict JTA implementations that expect
			// the close call before transaction completion.
			if (!this.connectionHolder.isOpen()) {
				TransactionSynchronizationManager.unbindResource(this.dataSource);
				this.holderActive = false;
				if (this.connectionHolder.hasConnection()) {
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
				}
			}
		}

		@Override
		public void afterCompletion(int status) {
			// If we haven't closed the Connection in beforeCompletion,
			// close it now. The holder might have been used for other
			// cleanup in the meantime, for example by a Hibernate Session.
			if (this.holderActive) {
				// The thread-bound ConnectionHolder might not be available anymore,
				// since afterCompletion might get called from a different thread.
				TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
				this.holderActive = false;
				if (this.connectionHolder.hasConnection()) {
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					// Reset the ConnectionHolder: It might remain bound to the thread.
					this.connectionHolder.setConnection(null);
				}
			}
			this.connectionHolder.reset();
		}
	}

}
