package com.khademni.service;

import com.khademni.model.UserModel;
import com.khademni.model.UserRole;
import java.sql.SQLException;

public class OfferService {

    public void createOffer(UserModel user, String title, String description, double price) throws SQLException {
        if (user.getCurrentMode() != UserRole.FREELANCER) {
            throw new IllegalStateException("Switch to Freelancer mode to create an offer.");
        }

        // TODO: Actual database implementation for creating offer
        System.out.println("Offer created for user: " + user.getId());
    }
}
