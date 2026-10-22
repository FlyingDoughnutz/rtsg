module com.example.rtsg {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires javafx.base;
    requires javafx.graphics;

    exports com.example.rtsg.ui.menu;
    opens com.example.rtsg.ui.menu to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;

    exports com.example.rtsg.app;
    opens com.example.rtsg.app to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.gameplay.buildings;
    opens com.example.rtsg.gameplay.buildings to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.config;
    opens com.example.rtsg.config to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.content;
    opens com.example.rtsg.content to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.entities;
    opens com.example.rtsg.entities to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.fog;
    opens com.example.rtsg.fog to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.input;
    opens com.example.rtsg.input to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.player;
    opens com.example.rtsg.player to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.gameplay.resources;
    opens com.example.rtsg.gameplay.resources to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.rules;
    opens com.example.rtsg.rules to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.scenario;
    opens com.example.rtsg.scenario to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.gameplay.structures;
    opens com.example.rtsg.gameplay.structures to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.technology;
    opens com.example.rtsg.technology to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.ui.dialogs;
    opens com.example.rtsg.ui.dialogs to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.ui.hud;
    opens com.example.rtsg.ui.hud to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.ui.sprites;
    opens com.example.rtsg.ui.sprites to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.gameplay.units;
    opens com.example.rtsg.gameplay.units to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
    exports com.example.rtsg.world;
    opens com.example.rtsg.world to javafx.fxml, com.almasb.fxgl.core, com.almasb.fxgl.entity, com.fasterxml.jackson.databind;
}
