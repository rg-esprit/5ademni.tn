module com.khademni {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.khademni to javafx.fxml;
    opens com.khademni.controller to javafx.fxml;
    opens com.khademni.model to javafx.fxml;
    exports com.khademni;
    exports com.khademni.controller;
    exports com.khademni.model;
}
