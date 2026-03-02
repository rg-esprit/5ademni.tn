package com.khademni.utils;

import com.khademni.model.UserModel;

public class SessionManager {
    private static UserModel currentUser;

    public static void setCurrentUser(UserModel user) {
        currentUser = user;
    }

    public static UserModel getCurrentUser() {
        return currentUser;
    }
}