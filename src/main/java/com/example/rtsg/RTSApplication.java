package com.example.rtsg;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.Viewport;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.example.rtsg.Resources.ResourceComponent;
import com.example.rtsg.Structures.StructureComponent;
import com.example.rtsg.Units.UnitComponent;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
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
    private HBox topUIBar;
    private Label mineralLabel;
    private Label gasLabel;
    private Label supplyLabel;

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

    private Cursor currCursor = javafx.scene.Cursor.DEFAULT;

    private final int MAP_BORDER_THRESHOLD = 10;
    private final double CAMERA_SPEED_MOUSEOVER = 500.0;
    private final double CAMERA_SPEED_MANUAL = 15.0;
    private final int MAP_HEIGHT = 3000;
    private final int MAP_WIDTH = 3000;

    private Player botPlayer1;

    private Player resourcePlayer;

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
        PlayerManager.setHumanPlayer(new Player("commander", Color.GREEN));
        PlayerManager.addToPlayerList(PlayerManager.getHumanPlayer());

        botPlayer1 = new Player("swarm", Color.DEEPPINK);
        PlayerManager.getHumanPlayer().changeRelation(botPlayer1, Relation.HOSTILE);
        botPlayer1.changeRelation(PlayerManager.getHumanPlayer(), Relation.HOSTILE);
        PlayerManager.addToPlayerList(botPlayer1);

        resourcePlayer = new Player("resource", Color.LIGHTBLUE);
        resourcePlayer.changeRelation(PlayerManager.getHumanPlayer(), Relation.NEUTRAL);
        resourcePlayer.changeRelation(botPlayer1, Relation.NEUTRAL);
        PlayerManager.addToPlayerList(resourcePlayer);

        //spawn sample first unit
        spawn("marine", new SpawnData(100, 100).put("owner", PlayerManager.getHumanPlayer()));
        spawn("marine", new SpawnData(100, 130).put("owner", PlayerManager.getHumanPlayer()));
        spawn("marine", new SpawnData(100, 160).put("owner", PlayerManager.getHumanPlayer()));
        spawn("marine", new SpawnData(100, 190).put("owner", PlayerManager.getHumanPlayer()));
        spawn("marine", new SpawnData(100, 210).put("owner", PlayerManager.getHumanPlayer()));
        spawn("scv", new SpawnData(100, 270).put("owner", PlayerManager.getHumanPlayer()));
        spawn("scv", new SpawnData(100, 300).put("owner", PlayerManager.getHumanPlayer()));
        spawn("scv", new SpawnData(100, 330).put("owner", PlayerManager.getHumanPlayer()));
        spawn("command_centre", new SpawnData(300, 400).put("owner", PlayerManager.getHumanPlayer()));

        spawn("mineral_large", new SpawnData(100, 400).put("owner", resourcePlayer));
        spawn("mineral_large", new SpawnData(100, 450).put("owner", resourcePlayer));
        spawn("mineral_large", new SpawnData(100, 500).put("owner", resourcePlayer));

        //spawn("zergling", new SpawnData(800, 300).put("owner", botPlayer1));

        spawn("command_centre", new SpawnData(1300, 1300).put("owner", botPlayer1));
        spawn("command_centre", new SpawnData(2200, 200).put("owner", botPlayer1));
        spawn("command_centre", new SpawnData(500, 2200).put("owner", botPlayer1));
        spawn("command_centre", new SpawnData(2200, 2200).put("owner", botPlayer1));
    }

    @Override
    protected void initUI(){
        //top bar ui in helper
        initTopUI();

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
                else if (isPatrolMode){
                    handlePatrolCommand(mousePosWorld);
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
                performDragSelection(selectionRectangle);
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
        Cursor newCursor = javafx.scene.Cursor.DEFAULT;
        //mouse hover over effect
        Point2D mousePos = getInput().getMousePositionWorld();
        Rectangle2D selectionRectangle = new Rectangle2D(mousePos.getX(), mousePos.getY(), 5, 5);
        var targetList = getGameWorld().getEntitiesInRange(selectionRectangle);
        boolean isHovering = targetList.stream()
                .anyMatch(e -> e.isType(EntityType.UNIT) || e.isType(EntityType.STRUCTURE) || e.isType(EntityType.RESOURCE));
        if(!isHovering) {
            newCursor = Cursor.DEFAULT;
        }
        else {
            newCursor = Cursor.HAND;
        }
        if (newCursor != currCursor) {
            getGameScene().setCursor(newCursor);
            currCursor = newCursor;
        }


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
        else if (entity.hasComponent(ResourceComponent.class)) {
            return entity.getComponent(ResourceComponent.class);
        }
        return null;
    }

    private Entity findTargetAt(Point2D mousePos) {
        var targetList = getGameWorld().getEntitiesInRange(new Rectangle2D(mousePos.getX() - 10, mousePos.getY() - 10, 20, 20));

        Entity targetEntity = targetList.stream()
                .filter(e -> e.isType(EntityType.UNIT) || e.isType(EntityType.STRUCTURE) || e.isType(EntityType.RESOURCE))
                .filter(e -> !selectedUnits.contains(e))
                .findFirst()
                .orElse(null);
        return targetEntity;
    }

    private void handleMoveCommand(Point2D mousePos) {
        for(Entity unit : selectedUnits) {
            unit.getComponentOptional(UnitComponent.class).ifPresent(component -> {
                if (PlayerManager.findPlayerFromID(component.getOwner().getPlayerID()).getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
                    component.moveTo(mousePos);
                }
                else System.out.println("Cannot move hostile units!");
            });
        }
        isMoveMode = false;
        getGameScene().setCursor(Cursor.DEFAULT);
    }

    private void handleAtkCommand(Point2D mousePos) {
        Entity target = findTargetAt(mousePos);
        for (Entity unit : selectedUnits) {
            unit.getComponentOptional(UnitComponent.class).ifPresent(component -> {
                if (PlayerManager.findPlayerFromID(component.getOwner().getPlayerID()).getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
                    if (target != null) {
                        component.attack(target);
                    }
                    else {
                        component.moveAttack(mousePos);
                    }
                }
            });
        }
        this.isAtkMode = false;
        getGameScene().setCursor(Cursor.DEFAULT); // Reset cursor
    }

    private void handlePatrolCommand(Point2D mousePos) {
        for (Entity unit : selectedUnits) {
            unit.getComponentOptional(UnitComponent.class).ifPresent(component -> {
               if (PlayerManager.findPlayerFromID(component.getOwner().getPlayerID()).getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
                   component.orderPatrol(mousePos);
               }
            });
        }
        isPatrolMode = false;
        getGameScene().setCursor(Cursor.DEFAULT);
    }

    private void handleRightClick(Point2D mousePos) {
        Entity targetEntity = findTargetAt(mousePos);
        RightClickCommandHelper.executeSmartCommand(selectedUnits, targetEntity, mousePos);
        if (targetEntity != null) {
            System.out.println("Right-Click Hit: " + targetEntity.getType());
        } else {
            System.out.println("Right-Click Hit: Ground at " + mousePos);
        }

    }

    private void performDragSelection(Rectangle2D selectionRect) {

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
        if(PlayerManager.findPlayerFromID(getEntityComponent(selectedEntity).getOwner().getPlayerID()).getStance(PlayerManager.getHumanPlayer()) == Relation.SELF) {
            //if selected entity is unit
            if (selectedEntity.hasComponent(UnitComponent.class)) {
                UnitComponent unitComp = selectedEntity.getComponent(UnitComponent.class);
                moveButton = createCommandButton("Move");
                moveButton.setOnAction(event -> {
                    isMoveMode = true;
                    isAtkMode = false;
                    isPatrolMode = false;
                    getGameScene().setCursor(Cursor.CROSSHAIR);
                });
                controlPanel.add(moveButton, 0, 0);

                stopButton = createCommandButton("Stop");
                stopButton.setOnMouseClicked(e -> {
                    for (Entity unit : selectedUnits) {
                        if (unit.hasComponent(UnitComponent.class)) {
                            //issues an overriding command to move the curr pos, effectively stopping it.
                            unit.getComponent(UnitComponent.class).moveTo(unit.getPosition());
                        }
                    }
                    isMoveMode = false;
                    isAtkMode = false;
                    isPatrolMode = false;
                    getGameScene().setCursor(Cursor.DEFAULT);
                });
                controlPanel.add(stopButton, 1, 0);

                atkButton = createCommandButton("Atk");
                atkButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        isAtkMode = true;
                        isMoveMode = false;
                        isPatrolMode = false;
                        getGameScene().setCursor(Cursor.CROSSHAIR);
                    }
                });
                controlPanel.add(atkButton, 2, 0);

                patrolButton = createCommandButton("Patrol");
                patrolButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        isPatrolMode = true;
                        isMoveMode = false;
                        isAtkMode = false;
                        getGameScene().setCursor(Cursor.CROSSHAIR);
                    }
                });
                controlPanel.add(patrolButton, 0, 1);

                holdButton = createCommandButton("Hold");
                holdButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        for (Entity unit : selectedUnits) {
                            unit.getComponentOptional(UnitComponent.class).ifPresent(UnitComponent::orderHold);
                        }
                        isMoveMode = false;
                        isAtkMode = false;
                        isPatrolMode = false;
                        getGameScene().setCursor(Cursor.DEFAULT);
                    }
                });
                controlPanel.add(holdButton, 1, 1);
                //worker specific buttons
                if (unitComp.isWorker()) {
                    Button returnButton = createCommandButton("Return");
                    returnButton.setOnAction(e -> {
                        issueReturnCommand(selectedUnits);
                    });
                    controlPanel.add(returnButton, 2, 1);
                }
            }
            //if selected entity is structure
            else if (selectedEntity.hasComponent(StructureComponent.class)) {
                //get
                StructureComponent structureComponent = selectedEntity.getComponent(StructureComponent.class);
                List<RTSCommand> actions = structureComponent.getCommandsList();

                //fills up from top left
                int col = 0;
                int row = 0;
                
                for  (RTSCommand command : actions) {
                    Button button = createCommandButton(command.getName());
                    button.setOnAction(event -> command.execute());
                    
                    controlPanel.add(button, col, row);
                    //go through all 9 spots
                    col++;
                    if (col > 2){
                        col = 0;
                        row++;
                    }
                }
                if (row > 2 && col > 2){
                    System.out.println("Too many commands!");
                }
            }
        }
        else if (PlayerManager.findPlayerFromID(getEntityComponent(selectedEntity).getOwner().getPlayerID()).getStance(PlayerManager.getHumanPlayer()) == Relation.HOSTILE) {
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

    private Button createCommandButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(90, 90);

        // Define the styles as strings
        String styleNormal = "-fx-background-color: #333333; -fx-text-fill: white; -fx-border-color: gold; -fx-border-width: 2px; -fx-font-weight: bold; -fx-font-size: 14px;";
        String styleHover  = "-fx-background-color: #444444; -fx-text-fill: white; -fx-border-color: #FFD700; -fx-border-width: 2px; -fx-font-weight: bold; -fx-font-size: 14px;";
        String stylePress  = "-fx-background-color: #111111; -fx-text-fill: #AAAAAA; -fx-border-color: #B8860B; -fx-border-width: 2px; -fx-font-weight: bold; -fx-font-size: 14px;";

        // Set initial style
        btn.setStyle(styleNormal);

        // Add interactions
        btn.setOnMouseEntered(e -> {
            if (!btn.isPressed()) btn.setStyle(styleHover);
        });

        btn.setOnMouseExited(e -> {
            if (!btn.isPressed()) btn.setStyle(styleNormal);
        });

        btn.setOnMousePressed(e -> btn.setStyle(stylePress));

        btn.setOnMouseReleased(e -> {
            // If mouse is still over the button, go back to hover, else normal
            if (btn.isHover()) btn.setStyle(styleHover);
            else btn.setStyle(styleNormal);
        });

        return btn;
    }

    private void issueReturnCommand(List<Entity> selectedUnits) {
        for (Entity unit : selectedUnits) {
           unit.getComponentOptional(UnitComponent.class).ifPresent(comp -> {
               if (comp.isWorker() && comp.hasCargo()) {
                   Entity nearestBase = comp.scanAroundCC();
                   if (nearestBase != null) {
                       comp.orderReturnCargo(nearestBase);
                   }
               }
           });
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

    private void initTopUI(){
        //bar itself (no bg)
        topUIBar = new HBox(20);
        topUIBar.setPrefSize(FXGL.getAppWidth(), 40);
        topUIBar.setPadding(new Insets(5, 20, 5, 20));
        topUIBar.setAlignment(Pos.CENTER_RIGHT);
        topUIBar.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0.7), CornerRadii.EMPTY, Insets.EMPTY)));

        //minerals
        HBox mineralBox = createResourceGroup(Color.CYAN, "0");
        mineralLabel = (Label) mineralBox.getChildren().get(1);

        //gas
        HBox gasBox = createResourceGroup(Color.LIMEGREEN, "0");
        gasLabel = (Label) gasBox.getChildren().get(1);

        //supply
        HBox supplyBox = createResourceGroup(Color.WHITE, "0/200");
        supplyLabel = (Label) supplyBox.getChildren().get(1);

        //add all to topuibar
        topUIBar.getChildren().addAll(mineralBox,gasBox,supplyBox);

        //bind labels to human
        Player human = PlayerManager.getHumanPlayer();
        mineralLabel.textProperty().bind(human.mineralsProperty().asString());
        gasLabel.textProperty().bind(human.gasProperty().asString());
        human.currentSupplyProperty().addListener((observable, oldValue, newValue) -> {
            updateSupplyLabel(human);
        });
        human.maxSupplyProperty().addListener((obs, o, n) -> {
            updateSupplyLabel(human);
        });
        //add it
        addUINode(topUIBar);
    }

    //helper to create icon-resource hbox
    private HBox createResourceGroup(Color color, String text) {
        HBox resourceGroup = new HBox(10);
        resourceGroup.setAlignment(Pos.CENTER_LEFT);

        Rectangle icon = new Rectangle(20, 20, color);
        icon.setStroke(Color.GRAY);

        // New way
        //var iconTexture = FXGL.texture("mineral_icon.png", 20, 20); // Resizes to 20x20
        //resourceGroup.getChildren().addAll(iconTexture, label);

        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold");

        resourceGroup.getChildren().addAll(icon, label);
        return resourceGroup;
    }

    //helper to update and format supply
    private void updateSupplyLabel(Player player) {
        supplyLabel.setText(player.getCurrentSupply() + " / " + player.getMaxSupply());
    }

}
