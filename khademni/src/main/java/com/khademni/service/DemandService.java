package com.khademni.service;

import com.khademni.model.UserModel;
import com.khademni.model.UserRole;
import java.sql.SQLException;

public class DemandService {

    public void createDemand(UserModel user, String title, String description, double budget) throws SQLException {
        if (user.getCurrentMode() != UserRole.CLIENT) {
            throw new IllegalStateException("Switch to Client mode to create a demand.");
        }

        // TODO: Actual database implementation for creating demand
        System.out.println("Demand created for user: " + user.getId());
    }
}
