package com.khademni.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDataBase {

    // Database credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/appdb";
    private static final String USER = "root";
    private static final String PASS = "";

    private static Connection connection = null;

    private MyDataBase() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            synchronized (MyDataBase.class) {
                if (connection == null || connection.isClosed()) {
                    try {
                        connection = DriverManager.getConnection(DB_URL, USER, PASS);
                    } catch (SQLException e) {
                        throw e;
                    }
                }
            }
        }
        return connection;
    }

    public static void resetAutoIncrementIfEmpty(String tableName, int startValue) {
        String countQuery = "SELECT COUNT(*) FROM " + tableName;
        String resetQuery = "ALTER TABLE " + tableName + " AUTO_INCREMENT = " + startValue;
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(countQuery);
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute(resetQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
