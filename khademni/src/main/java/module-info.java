module com.khademni {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.fontawesome5;
    requires javafx.graphics;
    requires jakarta.mail;
    requires org.json; 
    
requires org.kordamp.ikonli.materialdesign2;

    opens com.khademni to javafx.fxml;
    opens com.khademni.controller to javafx.fxml;
    opens com.khademni.model to javafx.fxml;
    exports com.khademni;
    exports com.khademni.controller;
    exports com.khademni.model;
}
