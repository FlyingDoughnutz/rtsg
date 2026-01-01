module com.example.rtsg {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;

    opens com.example.rtsg to javafx.fxml, com.almasb.fxgl.core;
    exports com.example.rtsg;
}