module com.khademni {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires java.net.http;
    requires java.sql;
    requires java.desktop;          // BufferedImage, ImageIO
    requires mysql.connector.j;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.fontawesome5;
    requires com.google.gson;       // JSON parsing of backend responses

    opens com.khademni to javafx.fxml;
    opens com.khademni.controller to javafx.fxml;
    opens com.khademni.model to javafx.fxml;

    exports com.khademni;
    exports com.khademni.controller;
    exports com.khademni.model;
}
