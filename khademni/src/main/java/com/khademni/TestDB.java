package com.khademni;

import com.khademni.utils.MyDataBase;
import java.sql.Connection;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement()) {

            int updated = stmt
                    .executeUpdate("UPDATE users SET current_mode = 'FREELANCER' WHERE current_mode = 'FREELANCERR'");
            System.out.println("Fixed " + updated + " corrupted user roles.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
