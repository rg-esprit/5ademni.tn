package com.khademni;

import com.khademni.utils.MyDataBase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class TestRegistration {
    public static void main(String[] args) {
        String sql = "INSERT INTO users (unique_id, current_mode, first_name, last_name, date_of_birth, email, password, bio, profile_image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, 8888);
            ps.setString(2, "CLIENT");
            ps.setString(3, "Test");
            ps.setString(4, "User");
            System.out.println("Executing ps.setDate(5, null)...");
            ps.setDate(5, null);
            ps.setString(6, "testdob@test.com");
            ps.setString(7, "Test1234");
            ps.setString(8, "Bio");
            ps.setString(9, "profile.jpg");

            ps.executeUpdate();
            System.out.println("Registration Successful with null DOB!");

            // cleanup
            conn.createStatement().executeUpdate("DELETE FROM users WHERE email='testdob@test.com'");

        } catch (Exception e) {
            System.out.println("Registration Failed:");
            e.printStackTrace();
        }
    }
}
