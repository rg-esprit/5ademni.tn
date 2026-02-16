module com.khademni {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires java.sql;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires mysql.connector.j;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.fontawesome5;
    requires com.github.librepdf.openpdf;

    opens com.khademni to javafx.fxml;
    opens com.khademni.controller to javafx.fxml;
    opens com.khademni.model to javafx.fxml;

    exports com.khademni;
    exports com.khademni.controller;
    exports com.khademni.model;
}
