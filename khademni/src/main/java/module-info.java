module com.khademni {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.khademni to javafx.fxml;
    exports com.khademni;
}
