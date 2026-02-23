package com.khademni.service;

import com.khademni.service.impl.UserServiceImpl;
import com.khademni.service.impl.OffreServiceImpl;
import com.khademni.service.impl.ContratServiceImpl;

/**
 * Simple Service Factory/Locator.
 * In a more complex app, this would be replaced by a proper DI container
 * (Spring/CDI).
 */
public class ServiceFactory {
    private static final UserService userService = new UserServiceImpl();
    private static final OffreService offreService = new OffreServiceImpl();
    private static final ContratService contratService = new ContratServiceImpl();

    public static UserService getUserService() {
        return userService;
    }

    public static OffreService getOffreService() {
        return offreService;
    }

    public static ContratService getContratService() {
        return contratService;
    }
}
