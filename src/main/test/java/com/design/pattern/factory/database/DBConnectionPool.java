package com.design.pattern.factory.database;


import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class DBConnectionPool extends Pool {

    private int checkedOut;//正在使用的连接数


    private Vector<Connection> freeConnections = new Vector<Connection>();//存放产生的连接数

    private String passWord = null;//密码
    private String url;     //连接字符串
    private String userName; // 用户名
    private static int num = 0;     //空连接数
    private static int numActive = 0;//当前连接数
    private static DBConnectionPool pool = null;//连接池实际变量

    public static synchronized DBConnectionPool getInstance() {
        if (pool == null) {
            pool = new DBConnectionPool();
        }
        return pool;
    }


    private DBConnectionPool() {
        try {
            init();
            for (int i = 0; i < normalConnect; i++) {
                Connection c = newConnection();
                if (c != null) {
                    freeConnections.addElement(c);
                    num++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            InputStream is = DBConnectionPool.class.getResourceAsStream(propertiesName);
            Properties p = new Properties();
            p.load(is);
            this.userName = p.getProperty("userName");
            this.passWord = p.getProperty("passWord");
            this.driverName = p.getProperty("driverName");
            this.url = p.getProperty("url");
            this.maxConnect = Integer.parseInt(p.getProperty("maxConnect"));
            this.normalConnect = Integer.parseInt(p.getProperty("normalConnect"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Connection newConnection() {
        Connection conn = null;
        try {
            if (userName == null) {
                conn = DriverManager.getConnection(url);
            } else {
                conn = DriverManager.getConnection(url, userName, passWord);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }


    @Override
    public void createPool() {
        pool = new DBConnectionPool();
        if (pool == null) {
            System.out.println("创建连接失败");
        } else {
            System.out.println("创建链接成功");
        }
    }

    @Override
    public synchronized Connection getConnection() {
        Connection con = null;
        if (freeConnections.size() > 0) {
            num--;
            con = (Connection) freeConnections.firstElement();
            freeConnections.removeElementAt(0);
            try {
                if (con.isClosed()) {
                    System.out.println("从连接池中删除一个无效连接");
                    con = getConnection();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                con = getConnection();
            }
        } else if (maxConnect == 0 || checkedOut < maxConnect) {
            con = newConnection();
        }
        if (con != null) {//当前连接数加1
            checkedOut++;
        }
        numActive++;
        return con;
    }

    @Override
    public Connection getConnection(long time) {
        long startTime = new Date().getTime();
        Connection con = null;
        while ((con = getConnection()) == null) {
            try {
                wait(time);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if ((new Date().getTime() - startTime) >= time) {
                return null;
            }
        }
        return con;
    }

    @Override
    public synchronized void freeConnection(Connection connection) {
        freeConnections.addElement(connection);
        num++;
        checkedOut--;
        numActive--;
        notifyAll();
    }


    @Override
    public int getNum() {
        return num;
    }

    @Override
    public int getNumActive() {
        return numActive;
    }


    public synchronized void release() {
        try {
            // 将当前连接赋值给枚举中
            Enumeration allConnections = freeConnections.elements();
            // 使用循环关闭所有的连接
            while (allConnections.hasMoreElements()) {
                Connection con = (Connection) allConnections.nextElement();
                try {
                    con.close();
                    num--;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            freeConnections.removeAllElements();
            numActive = 0;
        } catch (Exception e) {
            super.release();
        }
    }
}
