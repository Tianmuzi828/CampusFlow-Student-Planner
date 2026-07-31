module com.campusflow.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.campusflow.app to javafx.fxml;
    exports com.campusflow.app;
}
