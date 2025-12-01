package com.org.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PostgresPool {

    private static BlockingQueue<Connection> pool;
    private static int POOL_SIZE = 5; 
    private static String url;
    private static String user;
    private static String password;

    // Inicializa o pool
    public static void init(String host, int port, String database, String username, String pwd) throws SQLException {
        if (pool != null) return;

        url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        user = username;
        password = pwd;

        pool = new ArrayBlockingQueue<>(POOL_SIZE);

        for (int i = 0; i < POOL_SIZE; i++) {
            Connection conn = DriverManager.getConnection(url, user, password);
            pool.offer(conn);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (pool == null) throw new IllegalStateException("Pool não inicializado.");

        try {
            Connection conn = pool.take();
            if (conn.isClosed()) {
                conn = DriverManager.getConnection(url, user, password);
            }
            return conn;
        } catch (InterruptedException e) {
            throw new SQLException("Falha ao obter conexão do pool", e);
        }
    }

    public static void releaseConnection(Connection conn) {
        if (conn != null) {
            pool.offer(conn);
        }
    }

    public static void shutdown() {
        if (pool != null) {
            pool.forEach(conn -> {
                try { conn.close(); } catch (SQLException ignored) {}
            });
            pool.clear();
        }
    }
}
