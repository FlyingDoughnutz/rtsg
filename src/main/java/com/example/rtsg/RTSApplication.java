package com.example.rtsg;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.example.rtsg.Structures.StructureComponent;
import com.example.rtsg.Units.UnitComponent;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import static com.almasb.fxgl.dsl.FXGL.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RTSApplication extends GameApplication {

    private List<Entity> selectedUnits = new ArrayList<>();
    private Rectangle selectionBox = new Rectangle(0, 0, Color.rgb(0, 255, 0, 0.3));
    private Point2D selectionStartPoint;

    private boolean isMoveMode = false;

    private HBox bottomUIBar;
    private GridPane controlPanel;
    private HBox miniMapPanel;
    private VBox infoPanel;
    private Label nameLabel;
    private Label hpLabel;
    private Button moveButton;
    private Button stopButton;
    private Button atkButton;
    private Button patrolButton;
    private Button holdButton;

    @Override
    protected void initSettings(GameSettings gameSettings) {
        gameSettings.setWidth(1920);
        gameSettings.setHeight(1080);
        gameSettings.setTitle("JavaFX RTS");
        gameSettings.setPreserveResizeRatio(true);
        gameSettings.setManualResizeEnabled(true); // Allows the user to drag the window
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new StarCraftFactory());

        //spawn sample first unit
        spawn("marine", 100, 100);
        spawn("scv", 100, 200);
        spawn("zergling", 300, 300);
        spawn("command_centre", 500, 200);
    }

    @Override
    protected void initUI(){

        bottomUIBar = new HBox(5);
        bottomUIBar.setAlignment(Pos.BOTTOM_CENTER);
        bottomUIBar.setPrefWidth(FXGL.getAppWidth());
        bottomUIBar.setPrefHeight(300);
        bottomUIBar.setTranslateY(FXGL.getAppHeight() - 300);

        miniMapPanel = new HBox();
        miniMapPanel.setPrefSize(300,300);
        miniMapPanel.setBorder(new Border(new BorderStroke(Color.GOLD, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        miniMapPanel.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0.7), CornerRadii.EMPTY, Insets.EMPTY)));

        nameLabel = new Label("");
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        hpLabel = new Label("");
        hpLabel.setTextFill(Color.LIGHTGREEN);

        infoPanel = new VBox(5, nameLabel, hpLabel);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0.7), CornerRadii.EMPTY, Insets.EMPTY)));
        infoPanel.setPrefSize(800, 150);
        infoPanel.setMaxHeight(150);
        infoPanel.setBorder(new Border(new BorderStroke(Color.GOLD, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        HBox.setHgrow(infoPanel, Priority.ALWAYS);

        controlPanel = new GridPane();

        moveButton = new Button("Move");
        moveButton.setPrefSize(90,90);
        moveButton.setOnAction(event -> {
            if (!selectedUnits.isEmpty()) {
                isMoveMode = true;
            }
        });
        controlPanel.add(moveButton,0,0);

        stopButton = new Button("Stop");
        stopButton.setPrefSize(90,90);
        stopButton.setOnMouseClicked(e -> {
            for (Entity unit : selectedUnits){
                if (unit.hasComponent(UnitComponent.class)){
                    //issues an overriding command to move the curr pos, effectively stopping it.
                    unit.getComponent(UnitComponent.class).moveTo(unit.getPosition());
                }
            }
        });
        controlPanel.add(stopButton,1,0);

        atkButton = new Button("Atk");
        atkButton.setPrefSize(90,90);
        controlPanel.add(atkButton,2,0);

        patrolButton = new Button("Patrol");
        patrolButton.setPrefSize(90,90);
        controlPanel.add(patrolButton,0,1);

        holdButton = new Button("Hold");
        holdButton.setPrefSize(90,90);
        controlPanel.add(holdButton,1,1);

        controlPanel.setPrefSize(300, 300);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setHgap(5);
        controlPanel.setVgap(5);
        controlPanel.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0.7), CornerRadii.EMPTY, Insets.EMPTY)));
        controlPanel.setBorder(new Border(new BorderStroke(Color.GOLD, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        bottomUIBar.getChildren().addAll(miniMapPanel, infoPanel, controlPanel);
        addUINode(bottomUIBar);

        selectionBox.setStroke(Color.DARKGREEN);
        selectionBox.setStrokeWidth(1);
        selectionBox.setVisible(false);
        addUINode(selectionBox); //add to ui layer instead of render so it's on top
    }

    @Override
    protected void initInput() {
        Input input = getInput();

        UserAction selectionAction = new UserAction("Drag Selection") {
            //start drag
            @Override
            protected void onActionBegin() {
                if (isMoveMode) {
                    Point2D target = input.getMousePositionWorld();
                    for (Entity unit : selectedUnits){
                        unit.getComponent(UnitComponent.class).moveTo(target);
                    }
                    isMoveMode = false;
                }
                else{
                    selectionStartPoint = input.getMousePositionWorld();
                    selectionBox.setVisible(true);
                    selectionBox.setX(selectionStartPoint.getX());
                    selectionBox.setY(selectionStartPoint.getY());
                    selectionBox.setWidth(0);
                    selectionBox.setHeight(0);
                }
            }

            //updates box's dimensions while dragging
            @Override
            protected void onAction() {
                if (selectionBox.isVisible()) {
                    Point2D mousePos = input.getMousePositionWorld();
                    selectionBox.setX(Math.min(selectionStartPoint.getX(), mousePos.getX()));
                    selectionBox.setY(Math.min(selectionStartPoint.getY(), mousePos.getY()));
                    selectionBox.setWidth(Math.abs(selectionStartPoint.getX() - mousePos.getX()));
                    selectionBox.setHeight(Math.abs(selectionStartPoint.getY() - mousePos.getY()));
                }
            }

            //captures all units inside the box and adds it to the list of selected units
            @Override
            protected void onActionEnd() {
                var selectionRect = new Rectangle2D(selectionBox.getX(), selectionBox.getY(), selectionBox.getWidth(), selectionBox.getHeight());

                List<Entity> foundUnits = getGameWorld().getEntitiesInRange(selectionRect).stream().filter(e -> e.isType(EntityType.UNIT)||e.isType(EntityType.STRUCTURE)||e.isType(EntityType.RESOURCE)).toList();

                selectedUnits = new ArrayList<>(foundUnits);

                selectionBox.setVisible(false);

                nameLabel.textProperty().unbind();
                hpLabel.textProperty().unbind();
                nameLabel.setText("");
                hpLabel.setText("");

                if (!selectedUnits.isEmpty()) {

                    Entity firstUnit = selectedUnits.get(0);
                    EntityComponent targetComponent = null;

                    if (firstUnit.hasComponent(UnitComponent.class)) {
                        targetComponent = firstUnit.getComponent(UnitComponent.class);
                    }
                    else if(firstUnit.hasComponent(StructureComponent.class)) {
                        targetComponent = firstUnit.getComponent(StructureComponent.class);
                    }
                    if (targetComponent != null) {
                        nameLabel.textProperty().bind(targetComponent.nameProperty());
                        hpLabel.textProperty().bind(targetComponent.hpProperty().asString("HP: %d"));
                    }
                }
            }
        };

        input.addAction(selectionAction, MouseButton.PRIMARY);

        onBtnDown(MouseButton.SECONDARY, () -> {
            for(Entity unit : selectedUnits) {
                unit.getComponentOptional(UnitComponent.class).ifPresent(component -> {
                    component.moveTo(getInput().getMousePositionWorld());
                });
            }
        });
    }

}
