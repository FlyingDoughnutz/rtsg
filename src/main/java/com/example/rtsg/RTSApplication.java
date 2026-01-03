package com.example.rtsg;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.Viewport;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
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
    private boolean isAtkMode = false;
    private boolean isPatrolMode = false;
    private boolean isHoldMode = false;

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

    private Label controlOwnerLabel;
    private Label controlDescriptionLabel;

    private TextField commandInput;

    private final int MAP_BORDER_THRESHOLD = 10;
    private final double CAMERA_SPEED_MOUSEOVER = 500.0;
    private final double CAMERA_SPEED_MANUAL = 15.0;
    private final int MAP_HEIGHT = 3000;
    private final int MAP_WIDTH = 3000;

    private Player botPlayer1;

    @Override
    protected void initSettings(GameSettings gameSettings) {
        gameSettings.setWidth(1920);
        gameSettings.setHeight(1080);
        gameSettings.setTitle("JavaFX RTS");
        gameSettings.setVersion("1.0");
        gameSettings.setPreserveResizeRatio(true);
        gameSettings.setManualResizeEnabled(true); // Allows the user to drag the window
    }

    @Override
    protected void initGame() {

        getGameScene().getViewport().setBounds(0, 0, MAP_WIDTH, MAP_HEIGHT);

        entityBuilder()
                .at(0, 0)
                .view(new Rectangle(MAP_WIDTH, MAP_HEIGHT, Color.LIGHTGRAY))
                .zIndex(-1)
                .buildAndAttach();

        getGameWorld().addEntityFactory(new StarCraftFactory());

        PlayerManager.getInstance();
        PlayerManager.setHumanPlayer(new Player("Commander", Color.GREEN));
        PlayerManager.addToPlayerList(PlayerManager.getHumanPlayer());

        botPlayer1 = new Player("Zerus_Swarm", Color.DEEPPINK);
        PlayerManager.getHumanPlayer().changeRelation(botPlayer1, Relation.HOSTILE);
        botPlayer1.changeRelation(PlayerManager.getHumanPlayer(), Relation.HOSTILE);
        PlayerManager.addToPlayerList(botPlayer1);

        //spawn sample first unit
        spawn("marine", new SpawnData(100, 100).put("owner", PlayerManager.getHumanPlayer()));
        spawn("scv", new SpawnData(100, 200).put("owner", PlayerManager.getHumanPlayer()));
        spawn("scv", new SpawnData(100, 210).put("owner", PlayerManager.getHumanPlayer()));
        spawn("scv", new SpawnData(100, 220).put("owner", PlayerManager.getHumanPlayer()));

        spawn("zergling", new SpawnData(800, 300).put("owner", botPlayer1));
        //spawn("command_centre", new SpawnData(500, 200).put("owner", Owner.PLAYER));
        spawn("command_centre", new SpawnData(1300, 1300).put("owner", botPlayer1));
        spawn("command_centre", new SpawnData(2200, 200).put("owner", botPlayer1));
        spawn("command_centre", new SpawnData(500, 2200).put("owner", botPlayer1));
        spawn("command_centre", new SpawnData(2200, 2200).put("owner", botPlayer1));
    }

    @Override
    protected void initUI(){

        //command prompt
        commandInput = new TextField();
        commandInput.setPromptText("Enter command:");

        commandInput.setTranslateY(FXGL.getAppHeight() - 330);
        commandInput.setPrefWidth(FXGL.getAppWidth());
        commandInput.setVisible(false);

        commandInput.setOnAction(e -> {
            String commandStr = commandInput.getText();
            if(!commandStr.isEmpty()) {
                parseCommand(commandStr);
            }
            commandInput.clear();
            commandInput.setVisible(false);
            //return focus to game
            FXGL.getGameScene().getRoot().requestFocus();
        });
        addUINode(commandInput);

        //bottom controls
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

        Viewport view = getGameScene().getViewport();

        getInput().addAction(new UserAction("Move Camera Up") {
            @Override
            protected void onAction() {
                if(0 < view.getY()) view.setY(Math.max(0, view.getY() - CAMERA_SPEED_MANUAL));
            }
        }, KeyCode.UP);

        getInput().addAction(new UserAction("Move Camera Down") {
            @Override
            protected void onAction() {
                if (view.getY() < MAP_HEIGHT - view.getHeight()) view.setY(Math.min(MAP_HEIGHT - view.getHeight(), view.getY() + CAMERA_SPEED_MANUAL));
            }
        }, KeyCode.DOWN);

        getInput().addAction(new UserAction("Move Camera Left") {
            @Override
            protected void onAction() {
                if (0 < view.getX()) view.setX(Math.max(0, view.getX() - CAMERA_SPEED_MANUAL));
            }
        }, KeyCode.LEFT);

        getInput().addAction(new UserAction("Move Camera Right") {
            @Override
            protected void onAction() {
                if (view.getX() < MAP_WIDTH - view.getWidth()) view.setX(Math.min(MAP_WIDTH - view.getWidth(), view.getX() + CAMERA_SPEED_MANUAL));
            }
        }, KeyCode.RIGHT);

        //command console
        getInput().addAction(new UserAction("Open Console") {
            @Override
            protected void onActionBegin() {
                if (!commandInput.isVisible()) {
                    commandInput.setVisible(true);
                    commandInput.requestFocus();
                }
            }
        }, KeyCode.ENTER);

        //default left click drag select
        UserAction selectionAction = new UserAction("Drag Selection") {
            //start drag
            @Override
            protected void onActionBegin() {
                Point2D mousePosUI = input.getMousePositionUI();
                Point2D mousePosWorld = input.getMousePositionWorld();
                if (isMoveMode) {
                    handleMoveCommand(mousePosWorld);
                }
                else if (isAtkMode) {
                    handleAtkCommand(mousePosWorld);
                }
                else{
                    selectionStartPoint = mousePosUI;
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
                    Point2D mousePos = input.getMousePositionUI();
                    selectionBox.setX(Math.min(selectionStartPoint.getX(), mousePos.getX()));
                    selectionBox.setY(Math.min(selectionStartPoint.getY(), mousePos.getY()));
                    selectionBox.setWidth(Math.abs(selectionStartPoint.getX() - mousePos.getX()));
                    selectionBox.setHeight(Math.abs(selectionStartPoint.getY() - mousePos.getY()));
                }
            }

            //captures all units inside the box and adds it to the list of selected units
            @Override
            protected void onActionEnd() {
                //get the coords of the camera in the world
                double cameraX = getGameScene().getViewport().getX();
                double cameraY = getGameScene().getViewport().getY();
                //Add coord of camera to the selection box to get its position in game world
                var selectionRectangle = new Rectangle2D(
                        selectionBox.getX() + cameraX,
                        selectionBox.getY() + cameraY,
                        selectionBox.getWidth(),
                        selectionBox.getHeight()
                );
                performSelection(selectionRectangle);
                selectionBox.setVisible(false);
            }
        };

        input.addAction(selectionAction, MouseButton.PRIMARY);

        onBtnDown(MouseButton.SECONDARY, () -> {
            handleRightClick(input.getMousePositionWorld());
        });
    }

    @Override
    protected void onUpdate(double timePerFrame) {

        //mouse hover over effect


        //detection if mouse is at edges of screen
        double mousePosX = getInput().getMousePositionUI().getX();
        double mousePosY = getInput().getMousePositionUI().getY();

        Viewport view = getGameScene().getViewport();
        //move left
        if (mousePosX < MAP_BORDER_THRESHOLD && view.getX() > 0) {
            view.setX(Math.max(0, view.getX() - CAMERA_SPEED_MOUSEOVER * timePerFrame));
        }
        //move right
        if (mousePosX > FXGL.getAppWidth() - MAP_BORDER_THRESHOLD && view.getX() < MAP_WIDTH - view.getWidth()) {
            view.setX(Math.min(MAP_WIDTH - view.getWidth(), view.getX() + CAMERA_SPEED_MOUSEOVER * timePerFrame));
        }
        //move up
        if (mousePosY < MAP_BORDER_THRESHOLD && view.getY() > 0) {
            view.setY(Math.max(0, view.getY() - CAMERA_SPEED_MOUSEOVER * timePerFrame));
        }
        //move down
        if (mousePosY > FXGL.getAppHeight() - MAP_BORDER_THRESHOLD && view.getY() < MAP_HEIGHT - view.getHeight()) {
            view.setY(Math.min(MAP_HEIGHT - view.getHeight(), view.getY() + CAMERA_SPEED_MOUSEOVER * timePerFrame));
        }
    }

    private EntityComponent getEntityComponent(Entity entity) {

        if (entity.hasComponent(UnitComponent.class)) {
            return entity.getComponent(UnitComponent.class);
        }
        else if (entity.hasComponent(StructureComponent.class)) {
            return entity.getComponent(StructureComponent.class);
        }
        return null;
    }

    private Entity findTargetAt(Point2D mousePos) {
        var targetList = getGameWorld().getEntitiesInRange(new Rectangle2D(mousePos.getX() - 10, mousePos.getY() - 10, 20, 20));

        Entity targetEntity = targetList.stream()
                .filter(e -> e.isType(EntityType.UNIT) || e.isType(EntityType.STRUCTURE))
                .filter(e -> !selectedUnits.contains(e))
                .findFirst()
                .orElse(null);
        return targetEntity;
    }

    private void handleMoveCommand(Point2D mousePos) {
        for(Entity unit : selectedUnits) {
            unit.getComponentOptional(UnitComponent.class).ifPresent(component -> {
                if (component.getOwner().getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
                    component.moveTo(mousePos);
                }
                else System.out.println("Cannot move hostile units!");
            });
        }
        isMoveMode = false;
    }

    private void handleAtkCommand(Point2D mousePos) {
        Entity target = findTargetAt(mousePos);
        for (Entity unit : selectedUnits) {
            unit.getComponentOptional(UnitComponent.class).ifPresent(component -> {
                if (component.getOwner().getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
                    if (target != null) {
                        component.attack(target);
                    }
                    else {
                        component.moveTo(mousePos);
                    }
                }
            });
        }
        this.isAtkMode = false;
    }

    private void handleRightClick(Point2D mousePos) {
        Entity targetEntity = findTargetAt(mousePos);

        if (targetEntity != null) {
            System.out.println("Right-Click Hit: " + targetEntity.getType());
        } else {
            System.out.println("Right-Click Hit: Ground at " + mousePos);
        }
        for  (Entity unit : selectedUnits) {
            unit.getComponentOptional(UnitComponent.class).ifPresent(component -> {
                if (component.getOwner().getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
                    //clicked on unit
                    if(targetEntity != null) {
                        EntityComponent targetComp = getEntityComponent(targetEntity);
                        //if target has logic
                        Player targetOwner = targetComp.getOwner();
                        if (targetComp != null && PlayerManager.getHumanPlayer().getStance(targetOwner) == Relation.HOSTILE) {
                            System.out.println("Command: ATTACK");
                            component.attack(targetEntity);
                        }
                        else {
                            System.out.println("Command: FOLLOW/MOVE TO UNIT");
                            Point2D targetCenter = targetEntity.getCenter().subtract(unit.getWidth()/2, unit.getHeight()/2);
                            component.moveTo(targetCenter);
                        }
                    }
                    //clicked on empty ground
                    else{
                        System.out.println("Command: MOVE TO GROUND");
                        component.moveTo(getInput().getMousePositionWorld());
                    }
                }
            });
        }

    }

    private void performSelection(Rectangle2D selectionRect) {

        for (Entity e : selectedUnits){
            e.getComponentOptional(HealthBarComponent.class).ifPresent(bar -> bar.setIsSelected(false));
        }

        List<Entity> foundUnits = getGameWorld().getEntitiesInRange(selectionRect).stream().filter(e -> e.isType(EntityType.UNIT)||e.isType(EntityType.STRUCTURE)||e.isType(EntityType.RESOURCE)).toList();

        nameLabel.textProperty().unbind();
        hpLabel.textProperty().unbind();
        nameLabel.setText("");
        hpLabel.setText("");

        selectedUnits = new ArrayList<>(foundUnits);

        for (Entity e : selectedUnits){
            e.getComponentOptional(HealthBarComponent.class).ifPresent(bar -> bar.setIsSelected(true));
        }

        if (!selectedUnits.isEmpty()) {
            Entity firstUnit = selectedUnits.get(0);
            EntityComponent targetComponent = getEntityComponent(firstUnit);

            if (targetComponent != null) {
                nameLabel.textProperty().bind(targetComponent.nameProperty());
                hpLabel.textProperty().bind(targetComponent.hpProperty().asString("HP: %d"));
            }
            updateControlPanel(firstUnit);
        }
        else {
            controlPanel.getChildren().clear();
        }
    }

    private void updateControlPanel(Entity selectedEntity) {
        //clear existing buttons
        controlPanel.getChildren().clear();
        //check if unit belongs to player
        if(getEntityComponent(selectedEntity).getOwner().getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
            //if selected entity is unit
            if (selectedEntity.hasComponent(UnitComponent.class)) {
                moveButton = new Button("Move");
                moveButton.setPrefSize(90, 90);
                moveButton.setOnAction(event -> {
                    isMoveMode = true;
                });
                controlPanel.add(moveButton, 0, 0);

                stopButton = new Button("Stop");
                stopButton.setPrefSize(90, 90);
                stopButton.setOnMouseClicked(e -> {
                    for (Entity unit : selectedUnits) {
                        if (unit.hasComponent(UnitComponent.class)) {
                            //issues an overriding command to move the curr pos, effectively stopping it.
                            unit.getComponent(UnitComponent.class).moveTo(unit.getPosition());
                        }
                    }
                });
                controlPanel.add(stopButton, 1, 0);

                atkButton = new Button("Atk");
                atkButton.setPrefSize(90, 90);
                atkButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        isAtkMode = true;
                    }
                });
                controlPanel.add(atkButton, 2, 0);

                patrolButton = new Button("Patrol");
                patrolButton.setPrefSize(90, 90);
                patrolButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        isPatrolMode = true;
                    }
                });
                controlPanel.add(patrolButton, 0, 1);

                holdButton = new Button("Hold");
                holdButton.setPrefSize(90, 90);
                holdButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        isHoldMode = true;
                    }
                });
                controlPanel.add(holdButton, 1, 1);
            }
            //if selected entity is structure
            else if (selectedEntity.hasComponent(StructureComponent.class)) {
                Button btnTrain = new Button("Train Marine");
                btnTrain.setPrefSize(90, 90);
                btnTrain.setOnAction(e -> {
                    System.out.println("Training Marine...");
                    // Add training logic here later
                });

                controlPanel.add(btnTrain, 0, 0);
            }
        }
        else if (getEntityComponent(selectedEntity).getOwner().getStance(PlayerManager.getHumanPlayer()) == Relation.HOSTILE) {
            controlOwnerLabel = new Label("HOSTILE UNIT");
            controlOwnerLabel.setStyle("-fx-text-fill: red; -fx-font-size: 22px");
            controlDescriptionLabel = new Label("Cannot be commanded");
            controlDescriptionLabel.setStyle("-fx-text-fill: red; -fx-font-size: 20px");
            controlPanel.add(controlOwnerLabel, 0, 0);
            controlPanel.add(controlDescriptionLabel, 0, 1);
        }
        else {
            controlOwnerLabel = new Label("Neutral Unit");
            controlOwnerLabel.setStyle("-fx-text-fill: yellow; -fx-font-size: 22px");
            controlDescriptionLabel = new Label("Cannot be commanded");
            controlDescriptionLabel.setStyle("-fx-text-fill: yellow; -fx-font-size: 20px");
            controlPanel.add(controlOwnerLabel, 0, 0);
            controlPanel.add(controlDescriptionLabel, 0, 1);
        }
    }

    private void parseCommand (String input) {
        String[] tokens = input.trim().split("\\s+"); //split command by space
        if (tokens.length == 0) {return;}//do nothing if nothing is entered
        String command = tokens[0].toLowerCase();
        switch (command) {
            case "spawn":
                if (tokens.length < 2) {
                    System.out.println("Correct usage: spawn <name> <owner>(optional)");
                    return;
                }
                //gets the unit name to spawn
                String unitName = tokens[1];
                //defaults owner to player
                Player owner = PlayerManager.getHumanPlayer();
                //check if owner has been specified
                if (tokens.length > 2) {
                    try {
                        owner = PlayerManager.findPlayerFromID(tokens[2]);
                    }  catch (IllegalArgumentException e) {
                        System.out.println("Invalid owner. Correct usage: <owner> : <id# of player>");
                        return;
                    }
                }
                //tries to spawn unit at position
                try {
                    spawn(unitName, new SpawnData(getInput().getMousePositionWorld()).put("owner", owner));
                } catch (Exception e) {
                    System.out.println(unitName+ " is not a valid unit");
                }
                break;

            default:
                System.out.println(command + " is not a valid command");
                break;
        }

    }

}
