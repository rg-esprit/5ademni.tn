module com.khademni {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires java.prefs;
    requires javafx.web;
    requires com.fasterxml.jackson.databind;
    requires org.mariadb.jdbc;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.fontawesome5;
    requires com.github.librepdf.openpdf;
    requires stripe.java;
    requires bcrypt;
    requires org.slf4j;
    requires io.github.cdimascio.dotenv.java;
    requires jdk.jsobject;
    requires jakarta.mail;

    opens com.khademni to javafx.fxml;
    opens com.khademni.controller to javafx.fxml;
    opens com.khademni.model to javafx.fxml;
    opens com.khademni.service to javafx.fxml, javafx.base;
    opens com.khademni.dao to javafx.base;
    opens com.khademni.exception to javafx.base;

    exports com.khademni;
    exports com.khademni.controller;
    exports com.khademni.model;
    exports com.khademni.service;
    exports com.khademni.dao;
    exports com.khademni.exception;
    exports com.khademni.config;
    exports com.khademni.utils;
}
