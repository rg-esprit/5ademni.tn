package com.khademni.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ShowUsers {
    public static void main(String[] args) {
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt
                        .executeQuery(
                                "SELECT id, unique_id, first_name, last_name, current_mode FROM users LIMIT 15")) {

            System.out.println("--- Current Users in DB ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " | UniqueID: " + rs.getInt("unique_id") + " | Name: "
                        + rs.getString("first_name") + " "
                        + rs.getString("last_name") + " | Mode: " + rs.getString("current_mode"));
            }
            System.out.println("---------------------------");

            try (ResultSet rs2 = stmt
                    .executeQuery("SELECT id, client_id, freelancer_id, titre FROM contrats LIMIT 5")) {
                System.out.println("--- Current Contrats in DB ---");
                while (rs2.next()) {
                    System.out.println(
                            "ID: " + rs2.getInt("id") + " | ClientID: " + rs2.getInt("client_id") + " | FreelancerID: "
                                    + rs2.getInt("freelancer_id") + " | Title: " + rs2.getString("titre"));
                }
                System.out.println("------------------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
