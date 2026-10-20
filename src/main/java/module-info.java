module com.example.rtsg {
    requires javafx.controls;
    requires javafx.fxml;

    //note: the com.github.almasb:fxgl artifact declares the module name
    //com.almasb.fxgl.all, so this requires clause stays as-is
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires javafx.base;
    requires javafx.graphics;

    //FXGL reflectively instantiates components/factories and serialises them,
    //so every gameplay package has to be open, not just the root package
    opens com.example.rtsg to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    opens com.example.rtsg.Units to com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    opens com.example.rtsg.Structures to com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    opens com.example.rtsg.Resources to com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    opens com.example.rtsg.Buildings to com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg;
}