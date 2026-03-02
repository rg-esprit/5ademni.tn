package com.khademni.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckUsers {
    public static void main(String[] args) {
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, first_name, email FROM users")) {

            System.out.println("Available Users:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " | Name: " + rs.getString("first_name") + " | Email: "
                        + rs.getString("email"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
