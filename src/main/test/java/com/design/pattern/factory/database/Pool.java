package com.design.pattern.factory.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public abstract class Pool {

    public String propertiesName = "connection-INF.properties";

    private static Pool instance = null;


    protected int maxConnect = 100; //最大连接数
    protected int normalConnect = 10; //保持连接数
    protected String driverName = null;//驱动字符串
    protected Driver driver = null;//驱动变量

    protected Pool() {
        try {
            init();
            loadDrivers(driverName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void init() throws Exception {
        InputStream inputStream = Pool.class.getResourceAsStream(propertiesName);
        Properties properties = new Properties();
        properties.load(inputStream);
        this.driverName = properties.getProperty("driverName");
        this.maxConnect = Integer.parseInt(properties.getProperty("driverName"));
        this.normalConnect = Integer.parseInt(properties.getProperty("driverName"));
    }


    protected void loadDrivers(String driverName) {
        String driverClassName = driverName;
        try {
            driver = (Driver) Class.forName(driverClassName).newInstance();
            DriverManager.registerDriver(driver);
            System.out.println("成功注册jdbc驱动" + driverClassName);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("失败注册jdbc驱动" + driverClassName);
        }
    }


    public abstract void createPool();

    public static synchronized Pool getInstance() throws Exception {
        if (instance == null) {
            instance = (Pool) Class.forName("org.jdbc.sqlhelper.Pool").newInstance();
        }
        return instance;
    }

    //获得一个可用连接，如果没有则创建一个连接，且小于最小连接限制
    public abstract Connection getConnection();
    //
    public abstract Connection getConnection(long time);

    public abstract void freeConnection(Connection connection);

    public abstract int getNum();

    public abstract int getNumActive();

    protected synchronized void release(){

        try {
            DriverManager.deregisterDriver(driver);
            System.out.println("撤销JDBC欢动"+driver.getClass().getName()) ;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("无法撤销JDBC欢动"+driver.getClass().getName()) ;
        }
    }
}
