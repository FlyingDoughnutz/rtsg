package com.example.rtsg;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.Viewport;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.texture.Texture;
import com.example.rtsg.Buildings.BuildGrid;
import com.example.rtsg.Buildings.BuildingDatabase;
import com.example.rtsg.Buildings.BuildingPlacement;
import com.example.rtsg.Buildings.BuildingStats;
import com.example.rtsg.Buildings.ConstructionComponent;
import com.example.rtsg.Resources.ResourceComponent;
import com.example.rtsg.Structures.ProductionComponent;
import com.example.rtsg.Structures.RallyPointGeometry;
import com.example.rtsg.Structures.StructureComponent;
import com.example.rtsg.Units.UnitComponent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import static com.almasb.fxgl.dsl.FXGL.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RTSApplication extends GameApplication {

    private List<Entity> selectedUnits = new ArrayList<>();
    private Rectangle selectionBox = new Rectangle(0, 0, Color.rgb(0, 255, 0, 0.3));
    private Point2D selectionStartPoint;

    private enum InputMode { SELECT, MOVE, ATTACK, PATROL, PLACEMENT }
    private InputMode inputMode = InputMode.SELECT;

    //building placement raster grid - only visible while placing
    private Group gridLayer;
    private Rectangle placementGhost;
    private BuildingStats pendingBuilding;

    //rally point marker (red pin + dotted line), shown while the owning building is selected
    private Group rallyLayer;
    private Line rallyLine;
    private Group rallyPin;

    /** A snapped, validated candidate position for the building being placed. */
    private record PlacementPreview(BuildingStats building, int tileX, int tileY,
                                    Rectangle2D footprint, BuildingPlacement.Result result) {
    }

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
    private VBox productionPanel;
    private Label productionStatusLabel;
    private Label productionQueueLabel;
    private ProgressBar productionProgress;
    private Button cancelCurrentButton;
    private Button cancelLastButton;

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
        //Run a fixed 60 ticks per second. FXGL otherwise drives the loop from the
        //JavaFX pulse and only re-measures fps (and therefore tpf) every 2 seconds,
        //starting from a hardcoded 60 fps. On a display faster than 60 Hz that made
        //the simulation advance up to ~2.4x faster than wall time for the first two
        //seconds - the "everything suddenly speeds up at game start" effect.
        gameSettings.setTicksPerSecond(60);
    }

    @Override
    protected void initGame() {

        getGameScene().getViewport().setBounds(0, 0, MAP_WIDTH, MAP_HEIGHT);
        getGameWorld().getProperties().setValue("mapWidth", MAP_WIDTH);
        getGameWorld().getProperties().setValue("mapHeight", MAP_HEIGHT);

        entityBuilder()
                .at(0, 0)
                .view(new Rectangle(MAP_WIDTH, MAP_HEIGHT, Color.LIGHTGRAY))
                .zIndex(-1)
                .buildAndAttach();

        getGameWorld().addEntityFactory(new StarCraftFactory());
        createBuildGridOverlay();
        createRallyPointOverlay();

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
        spawnBuildingAtTiles("command_centre", 10, 13, PlayerManager.getHumanPlayer());
        spawnBuildingAtTiles("barracks", 23, 16, PlayerManager.getHumanPlayer());

        spawn("mineral_large", new SpawnData(100, 400).put("owner", resourcePlayer));
        spawn("mineral_large", new SpawnData(100, 450).put("owner", resourcePlayer));
        spawn("mineral_large", new SpawnData(100, 500).put("owner", resourcePlayer));
        spawn("mineral_large", new SpawnData(100, 550).put("owner", resourcePlayer));

        spawn("mineral_small", new SpawnData(130, 420).put("owner", resourcePlayer));
        spawn("mineral_small", new SpawnData(130, 470).put("owner", resourcePlayer));
        spawn("mineral_small", new SpawnData(130, 520).put("owner", resourcePlayer));
        spawn("mineral_small", new SpawnData(130, 570).put("owner", resourcePlayer));
        //spawn("zergling", new SpawnData(800, 300).put("owner", botPlayer1));

        //buildings sit on tile corners so they line up with the placement grid
        spawnBuildingAtTiles("command_centre", 43, 43, botPlayer1);
        spawnBuildingAtTiles("command_centre", 73, 6, botPlayer1);
        spawnBuildingAtTiles("command_centre", 16, 73, botPlayer1);
        spawnBuildingAtTiles("command_centre", 73, 73, botPlayer1);
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

        productionStatusLabel = new Label();
        productionStatusLabel.setTextFill(Color.WHITE);
        productionQueueLabel = new Label();
        productionQueueLabel.setTextFill(Color.LIGHTGRAY);
        productionProgress = new ProgressBar(0);
        productionProgress.setMaxWidth(Double.MAX_VALUE);
        cancelCurrentButton = new Button("Cancel current");
        cancelLastButton = new Button("Cancel last");
        productionPanel = new VBox(5,
                new HBox(15, productionStatusLabel, productionQueueLabel),
                productionProgress,
                new HBox(8, cancelCurrentButton, cancelLastButton));
        productionPanel.setVisible(false);
        productionPanel.setManaged(false);

        infoPanel = new VBox(5, nameLabel, hpLabel, productionPanel);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0.7), CornerRadii.EMPTY, Insets.EMPTY)));
        infoPanel.setPrefSize(600, 200);
        infoPanel.setMaxHeight(200);
        infoPanel.setBorder(new Border(new BorderStroke(Color.GOLD, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        HBox.setHgrow(infoPanel, Priority.ALWAYS);

        controlPanel = new GridPane();

        controlPanel.setPrefSize(500, 300);
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
        selectionBox.setMouseTransparent(true);
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
                selectionBox.setVisible(false);
                if (isPointerOverUI(mousePosUI)) {
                    return;
                }
                Point2D mousePosWorld = input.getMousePositionWorld();
                switch (inputMode) {
                    case MOVE -> handleMoveCommand(mousePosWorld);
                    case ATTACK -> handleAtkCommand(mousePosWorld);
                    case PATROL -> handlePatrolCommand(mousePosWorld);
                    case PLACEMENT -> tryPlaceBuilding(mousePosWorld);
                    case SELECT -> {
                        selectionStartPoint = mousePosUI;
                        selectionBox.setVisible(true);
                        selectionBox.setX(selectionStartPoint.getX());
                        selectionBox.setY(selectionStartPoint.getY());
                        selectionBox.setWidth(0);
                        selectionBox.setHeight(0);
                    }
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
                // Command clicks and UI clicks never start a selection gesture.
                if (!selectionBox.isVisible()) {
                    return;
                }
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
            if (isPointerOverUI(input.getMousePositionUI())) {
                return;
            }
            //right click cancels building placement instead of issuing a command
            if (inputMode == InputMode.PLACEMENT) {
                cancelPlacement();
                return;
            }
            handleRightClick(input.getMousePositionWorld());
        });

        //escape also leaves placement mode
        getInput().addAction(new UserAction("Cancel Placement") {
            @Override
            protected void onActionBegin() {
                if (inputMode == InputMode.PLACEMENT) {
                    cancelPlacement();
                }
            }
        }, KeyCode.ESCAPE);
    }

    @Override
    protected void onUpdate(double timePerFrame) {
        if (selectedUnits.stream().anyMatch(entity -> !isSelectable(entity))) {
            applySelection(selectedUnits.stream().filter(this::isSelectable).toList());
        }

        //keep the snapped ghost (and its validity colour) under the cursor
        if (inputMode == InputMode.PLACEMENT && !isPointerOverUI(getInput().getMousePositionUI())) {
            updatePlacementPreview(getInput().getMousePositionWorld());
        }

        //rally pin + dotted line follow selection and can be re-targeted live
        updateRallyPointOverlay();

        Cursor newCursor = javafx.scene.Cursor.DEFAULT;
        if (!isPointerOverUI(getInput().getMousePositionUI())) {
            if (inputMode != InputMode.SELECT) {
                newCursor = Cursor.CROSSHAIR;
            } else {
                Point2D mousePos = getInput().getMousePositionWorld();
                Rectangle2D selectionRectangle = new Rectangle2D(mousePos.getX(), mousePos.getY(), 5, 5);
                if (getGameWorld().getEntitiesInRange(selectionRectangle).stream().anyMatch(this::isSelectable)) {
                    newCursor = Cursor.HAND;
                }
            }
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

    private boolean isSelectable(Entity entity) {
        if (!entity.isActive()) {
            return false;
        }
        //construction sites carry no EntityComponent but are selectable so the
        //player can watch progress and cancel the build
        if (entity.isType(EntityType.CONSTRUCTION)) {
            return entity.hasComponent(ConstructionComponent.class);
        }
        EntityComponent component = getEntityComponent(entity);
        return component != null
                && (component instanceof ResourceComponent || component.hpProperty().get() > 0);
    }

    private boolean isPointerOverUI(Point2D mousePos) {
        return (bottomUIBar != null && bottomUIBar.getBoundsInParent().contains(mousePos))
                || (topUIBar != null && topUIBar.getBoundsInParent().contains(mousePos))
                || (commandInput != null && commandInput.isVisible()
                    && commandInput.getBoundsInParent().contains(mousePos));
    }

    private void setInputMode(InputMode mode) {
        inputMode = mode;
        currCursor = mode == InputMode.SELECT ? Cursor.DEFAULT : Cursor.CROSSHAIR;
        getGameScene().setCursor(currCursor);
    }

    private void forEachCommandableUnit(Consumer<UnitComponent> command) {
        for (Entity unit : selectedUnits) {
            RightClickCommandHelper.getCommandableUnit(unit).ifPresent(command);
        }
    }

    // ---------------------------------------------------------------- building placement

    /**
     * Builds the placement grid once. It spans the whole map, is pick-transparent
     * and stays hidden until the player enters placement mode.
     */
    private void createBuildGridOverlay() {
        gridLayer = new Group();
        gridLayer.setVisible(false);
        gridLayer.setMouseTransparent(true);

        Color minor = Color.rgb(255, 255, 255, 0.16);
        Color major = Color.rgb(255, 255, 255, 0.34);
        for (int x = 0; x <= MAP_WIDTH; x += BuildGrid.TILE_SIZE) {
            Line line = new Line(x, 0, x, MAP_HEIGHT);
            line.setStroke(x % (BuildGrid.TILE_SIZE * 5) == 0 ? major : minor);
            line.setStrokeWidth(x % (BuildGrid.TILE_SIZE * 5) == 0 ? 1.0 : 0.5);
            gridLayer.getChildren().add(line);
        }
        for (int y = 0; y <= MAP_HEIGHT; y += BuildGrid.TILE_SIZE) {
            Line line = new Line(0, y, MAP_WIDTH, y);
            line.setStroke(y % (BuildGrid.TILE_SIZE * 5) == 0 ? major : minor);
            line.setStrokeWidth(y % (BuildGrid.TILE_SIZE * 5) == 0 ? 1.0 : 0.5);
            gridLayer.getChildren().add(line);
        }

        placementGhost = new Rectangle(0, 0);
        placementGhost.setVisible(false);
        placementGhost.setMouseTransparent(true);
        placementGhost.setStrokeWidth(2);
        gridLayer.getChildren().add(placementGhost);

        entityBuilder().at(0, 0).view(gridLayer).zIndex(100).buildAndAttach();
    }

    // ---------------------------------------------------------------- rally points

    /**
     * Creates the rally point marker once: a small red pin plus a dotted line.
     * It lives in world space and is only shown while the owning building is
     * selected (see {@link #updateRallyPointOverlay()}).
     */
    private void createRallyPointOverlay() {
        rallyLayer = new Group();
        rallyLayer.setVisible(false);
        rallyLayer.setMouseTransparent(true);

        rallyLine = new Line();
        rallyLine.setStroke(Color.CRIMSON);
        rallyLine.setStrokeWidth(1.5);
        rallyLine.setOpacity(0.9);
        rallyLine.getStrokeDashArray().addAll(4.0, 6.0);

        Circle pinBody = new Circle(6, Color.CRIMSON);
        pinBody.setCenterX(0);
        pinBody.setCenterY(-12);
        pinBody.setStroke(Color.WHITE);
        pinBody.setStrokeWidth(1);
        //downward tip; local (0,2) is the point that marks the rally position
        Polygon pinTip = new Polygon(-5, -7, 5, -7, 0, 2);
        pinTip.setFill(Color.CRIMSON);
        pinTip.setStroke(Color.WHITE);
        pinTip.setStrokeWidth(1);
        rallyPin = new Group(pinBody, pinTip);

        rallyLayer.getChildren().addAll(rallyLine, rallyPin);
        entityBuilder().at(0, 0).view(rallyLayer).zIndex(96).buildAndAttach();
    }

    /**
     * Shows the rally marker for the first selected production building that has
     * a rally point, and hides it otherwise.
     */
    private void updateRallyPointOverlay() {
        if (rallyLayer == null) {
            return;
        }
        Entity building = null;
        Point2D rally = null;
        for (Entity selected : selectedUnits) {
            if (!selected.isActive()) {
                continue;
            }
            ProductionComponent production = selected.getComponentOptional(ProductionComponent.class).orElse(null);
            if (production != null && production.getRallyPoint() != null) {
                building = selected;
                rally = production.getRallyPoint();
                break;
            }
        }
        if (building == null) {
            rallyLayer.setVisible(false);
            return;
        }
        Rectangle2D bounds = new Rectangle2D(building.getX(), building.getY(),
                building.getWidth(), building.getHeight());
        Point2D start = RallyPointGeometry.edgePoint(bounds, rally);
        rallyLine.setStartX(start.getX());
        rallyLine.setStartY(start.getY());
        rallyLine.setEndX(rally.getX());
        rallyLine.setEndY(rally.getY());
        rallyPin.setTranslateX(rally.getX());
        //shift so the pin tip lands exactly on the rally position
        rallyPin.setTranslateY(rally.getY() - 2);
        rallyLayer.setVisible(true);
    }

    /** Right-clicking a selected production building sets its rally point. */
    private boolean setRallyPoint(Point2D mousePos) {
        for (Entity selected : selectedUnits) {
            if (!selected.isActive() || !canCommandStructure(selected)) {
                continue;
            }
            ProductionComponent production = selected.getComponentOptional(ProductionComponent.class).orElse(null);
            if (production != null) {
                production.setRallyPoint(mousePos);
                return true;
            }
        }
        return false;
    }

    private void spawnBuildingAtTiles(String buildingId, int tileX, int tileY, Player owner) {
        BuildingStats stats = BuildingDatabase.get(buildingId);
        spawn(stats.id(), new SpawnData(BuildGrid.tileOrigin(tileX), BuildGrid.tileOrigin(tileY))
                .put("owner", owner));
    }

    /** Enters placement mode for the given building (toggles off if already placing it). */
    private void startPlacement(BuildingStats building) {
        if (building == null) {
            return;
        }
        if (pendingBuilding == building) {
            cancelPlacement();
            return;
        }
        boolean hasBuilder = selectedUnits.stream()
                .anyMatch(unit -> RightClickCommandHelper.getCommandableUnit(unit)
                        .filter(UnitComponent::isWorker).isPresent());
        if (!hasBuilder) {
            System.out.println("Select a worker to place " + building.name());
            return;
        }
        pendingBuilding = building;
        setInputMode(InputMode.PLACEMENT);
        gridLayer.setVisible(true);
        placementGhost.setVisible(true);
        updatePlacementPreview(getInput().getMousePositionWorld());
    }

    /** Leaves placement mode and hides the grid. */
    private void cancelPlacement() {
        pendingBuilding = null;
        if (gridLayer != null) {
            gridLayer.setVisible(false);
        }
        if (placementGhost != null) {
            placementGhost.setVisible(false);
        }
        if (inputMode == InputMode.PLACEMENT) {
            setInputMode(InputMode.SELECT);
        }
    }

    private PlacementPreview evaluatePlacement(Point2D mouse) {
        if (pendingBuilding == null) {
            return null;
        }
        int tileX = BuildGrid.centeredTileIndex(mouse.getX(), pendingBuilding.tilesWide());
        int tileY = BuildGrid.centeredTileIndex(mouse.getY(), pendingBuilding.tilesHigh());
        Rectangle2D footprint = BuildingPlacement.footprintFor(pendingBuilding, tileX, tileY);
        Player human = PlayerManager.getHumanPlayer();
        BuildingPlacement.Result result = BuildingPlacement.evaluate(
                pendingBuilding, tileX, tileY, MAP_WIDTH, MAP_HEIGHT,
                collectOccupiedFootprints(), human == null ? 0 : human.getMinerals());
        return new PlacementPreview(pendingBuilding, tileX, tileY, footprint, result);
    }

    /** Snaps the preview ghost to the grid and colours it by validity. */
    private void updatePlacementPreview(Point2D mouse) {
        PlacementPreview preview = evaluatePlacement(mouse);
        if (preview == null) {
            return;
        }
        placementGhost.setX(preview.footprint().getMinX());
        placementGhost.setY(preview.footprint().getMinY());
        placementGhost.setWidth(preview.footprint().getWidth());
        placementGhost.setHeight(preview.footprint().getHeight());
        boolean valid = preview.result().isValid();
        placementGhost.setFill(valid ? Color.rgb(0, 255, 0, 0.28) : Color.rgb(255, 40, 40, 0.34));
        placementGhost.setStroke(valid ? Color.LIMEGREEN : Color.RED);
    }

    private List<Rectangle2D> collectOccupiedFootprints() {
        List<Rectangle2D> occupied = new ArrayList<>();
        for (EntityType type : List.of(EntityType.UNIT, EntityType.STRUCTURE,
                EntityType.RESOURCE, EntityType.CONSTRUCTION)) {
            for (Entity entity : getGameWorld().getEntitiesByType(type)) {
                if (entity.isActive()) {
                    occupied.add(new Rectangle2D(entity.getX(), entity.getY(),
                            entity.getWidth(), entity.getHeight()));
                }
            }
        }
        return occupied;
    }

    /** Places the pending building at the snapped cursor tile and orders workers to build it. */
    private void tryPlaceBuilding(Point2D mouse) {
        PlacementPreview preview = evaluatePlacement(mouse);
        if (preview == null) {
            return;
        }
        if (!preview.result().isValid()) {
            //stay in placement mode so another spot can be chosen
            System.out.println("[build] " + preview.building().name() + ": " + preview.result().message());
            return;
        }
        List<Entity> builders = selectedUnits.stream()
                .filter(unit -> RightClickCommandHelper.getCommandableUnit(unit)
                        .filter(UnitComponent::isWorker).isPresent())
                .toList();
        Player human = PlayerManager.getHumanPlayer();
        if (builders.isEmpty() || human == null) {
            cancelPlacement();
            return;
        }
        human.setMinerals(human.getMinerals() - preview.building().mineralCost());
        Entity site = spawn("construction_site",
                new SpawnData(preview.footprint().getMinX(), preview.footprint().getMinY())
                        .put("building", preview.building())
                        .put("owner", human)
                        .put("tileX", preview.tileX())
                        .put("tileY", preview.tileY()));
        for (Entity builder : builders) {
            builder.getComponent(UnitComponent.class).orderBuild(site);
        }
        cancelPlacement();
    }

    private Entity findTargetAt(Point2D mousePos) {
        //centre the hit test on the cursor: the old -10/+20 offset biased the
        //pick box to the bottom-right, so a click near a unit's top/left edge
        //missed it and strayed onto whatever was nearby
        var targetList = getGameWorld().getEntitiesInRange(new Rectangle2D(mousePos.getX() - 10, mousePos.getY() - 10, 20, 20));

        Entity targetEntity = targetList.stream()
                //construction sites are right-click targets (resume building) but
                //are not selectable, so they are admitted explicitly here
                .filter(e -> isSelectable(e) || e.isType(EntityType.CONSTRUCTION))
                // Prefer units and structures when a resource overlaps them.
                .filter(e -> !e.isType(EntityType.RESOURCE))
                .filter(e -> !selectedUnits.contains(e))
                .findFirst()
                .orElse(null);
        // Reuse the same hit test for gathering when no unit or structure was hit.
        if (targetEntity == null) {
            Entity resource = targetList.stream()
                    .filter(this::isSelectable)
                    .filter(e -> e.isType(EntityType.RESOURCE))
                    .findFirst()
                    .orElse(null);
            return resource;
        }
        return targetEntity;
    }

    private void handleMoveCommand(Point2D mousePos) {
        forEachCommandableUnit(component -> component.moveTo(mousePos));
        setInputMode(InputMode.SELECT);
    }

    private void handleAtkCommand(Point2D mousePos) {
        Entity target = findTargetAt(mousePos);
        forEachCommandableUnit(component -> {
            if (target != null && !target.isType(EntityType.RESOURCE)
                    && !target.isType(EntityType.CONSTRUCTION)) {
                component.orderAttack(target);
            } else {
                component.moveAttack(mousePos);
            }
        });
        setInputMode(InputMode.SELECT);
    }

    private void handlePatrolCommand(Point2D mousePos) {
        forEachCommandableUnit(component -> component.orderPatrol(mousePos));
        setInputMode(InputMode.SELECT);
    }

    private void handleRightClick(Point2D mousePos) {
        //a selected production building receives a rally point instead of a unit command
        if (setRallyPoint(mousePos)) {
            setInputMode(InputMode.SELECT);
            return;
        }
        Entity targetEntity = findTargetAt(mousePos);
        RightClickCommandHelper.executeSmartCommand(selectedUnits, targetEntity, mousePos);
        setInputMode(InputMode.SELECT);
    }

    private void performDragSelection(Rectangle2D selectionRect) {

        //a box this small means the player clicked rather than dragged, so pick
        //the single entity under the cursor instead of rebuilding the selection
        //from an empty drag rectangle
        if (selectionRect.getWidth() < 4 && selectionRect.getHeight() < 4) {
            Entity clicked = findSelectableAt(selectionRect.getMinX(), selectionRect.getMinY());
            if (clicked != null) {
                applySelection(List.of(clicked));
            }
            //the click will have been on empty ground; keep the current
            //selection and let the next real click decide
            return;
        }

        List<Entity> foundUnits = getGameWorld().getEntitiesInRange(selectionRect).stream()
                .filter(this::isSelectable).toList();

        applySelection(foundUnits);
    }

    //finds the topmost selectable entity at a world position
    private Entity findSelectableAt(double worldX, double worldY) {
        return getGameWorld().getEntitiesInRange(new Rectangle2D(worldX, worldY, 1, 1)).stream()
                .filter(this::isSelectable)
                .findFirst()
                .orElse(null);
    }

    //replaces the current selection and refreshes the info/command panels
    private void applySelection(List<Entity> newSelection) {
        setInputMode(InputMode.SELECT);
        for (Entity e : selectedUnits){
            e.getComponentOptional(HealthBarComponent.class).ifPresent(bar -> bar.setIsSelected(false));
        }

        nameLabel.textProperty().unbind();
        hpLabel.textProperty().unbind();
        nameLabel.setText("");
        hpLabel.setText("");
        clearProductionPanel();

        selectedUnits = new ArrayList<>(newSelection.stream().filter(this::isSelectable).toList());

        for (Entity e : selectedUnits){
            e.getComponentOptional(HealthBarComponent.class).ifPresent(bar -> bar.setIsSelected(true));
        }

        if (selectedUnits.isEmpty()) {
            controlPanel.getChildren().clear();
            return;
        }

        Entity firstUnit = selectedUnits.get(0);
        EntityComponent targetComponent = getEntityComponent(firstUnit);
        ConstructionComponent construction = firstUnit.getComponentOptional(ConstructionComponent.class).orElse(null);

        if (targetComponent != null) {
            nameLabel.textProperty().bind(targetComponent.nameProperty());
            hpLabel.textProperty().bind(targetComponent.hpProperty().asString("HP: %d"));
        }
        else if (construction != null) {
            nameLabel.setText(construction.getStats().name() + " (under construction)");
            hpLabel.textProperty().bind(construction.progressProperty()
                    .multiply(100).asString("Progress: %.0f%%"));
        }
        bindProductionPanel(firstUnit);
        updateControlPanel(firstUnit);
    }

    private void clearProductionPanel() {
        productionStatusLabel.textProperty().unbind();
        productionQueueLabel.textProperty().unbind();
        productionProgress.progressProperty().unbind();
        cancelCurrentButton.disableProperty().unbind();
        cancelLastButton.disableProperty().unbind();
        cancelCurrentButton.setOnAction(null);
        cancelLastButton.setOnAction(null);
        productionPanel.setVisible(false);
        productionPanel.setManaged(false);
    }

    private void bindProductionPanel(Entity selectedEntity) {
        if (!PlayerManager.isOwnedByHuman(getEntityComponent(selectedEntity))) {
            return;
        }
        selectedEntity.getComponentOptional(ProductionComponent.class).ifPresent(production -> {
            productionStatusLabel.textProperty().bind(production.statusProperty());
            productionQueueLabel.textProperty().bind(production.queueSizeProperty().asString("Queued: %d"));
            productionProgress.progressProperty().bind(production.progressProperty());
            cancelCurrentButton.disableProperty().bind(production.queueSizeProperty().isEqualTo(0));
            cancelLastButton.disableProperty().bind(production.queueSizeProperty().isEqualTo(0));
            cancelCurrentButton.setOnAction(event -> {
                if (canCommandStructure(selectedEntity)) {
                    production.cancelCurrentUnit();
                }
            });
            cancelLastButton.setOnAction(event -> {
                if (canCommandStructure(selectedEntity)) {
                    production.cancelLastUnit();
                }
            });
            productionPanel.setVisible(true);
            productionPanel.setManaged(true);
        });
    }

    private boolean canCommandStructure(Entity entity) {
        return isSelectable(entity) && entity.hasComponent(StructureComponent.class)
                && PlayerManager.isOwnedByHuman(getEntityComponent(entity));
    }

    private void updateControlPanel(Entity selectedEntity) {
        //clear existing buttons
        controlPanel.getChildren().clear();

        //construction sites are selected so they can be inspected and cancelled
        ConstructionComponent construction = selectedEntity.getComponentOptional(ConstructionComponent.class).orElse(null);
        if (construction != null) {
            if (PlayerManager.isOwnedByHuman(construction.getOwner())) {
                Button cancelBuild = createCommandButton("Cancel");
                cancelBuild.setOnAction(event -> cancelConstruction(construction));
                controlPanel.add(cancelBuild, 0, 0);
            }
            else {
                controlOwnerLabel = new Label("UNDER CONSTRUCTION");
                controlOwnerLabel.setStyle("-fx-text-fill: red; -fx-font-size: 20px");
                controlDescriptionLabel = new Label("Cannot be commanded");
                controlDescriptionLabel.setStyle("-fx-text-fill: red; -fx-font-size: 17px");
                controlPanel.add(controlOwnerLabel, 0, 0);
                controlPanel.add(controlDescriptionLabel, 0, 1);
            }
            return;
        }

        EntityComponent selectedComponent = getEntityComponent(selectedEntity);
        //resources (or anything without gameplay logic) may still be inside the
        //drag-selection rectangle, so bail out instead of dereferencing null
        if (selectedComponent == null) {
            return;
        }

        //check if unit belongs to player
        if (PlayerManager.isOwnedByHuman(selectedComponent)) {
            //if selected entity is unit
            if (selectedEntity.hasComponent(UnitComponent.class)) {
                UnitComponent unitComp = selectedEntity.getComponent(UnitComponent.class);
                moveButton = createCommandButton("Move");
                moveButton.setOnAction(event -> {
                    setInputMode(InputMode.MOVE);
                });
                controlPanel.add(moveButton, 0, 0);

                stopButton = createCommandButton("Stop");
                stopButton.setOnAction(e -> {
                    forEachCommandableUnit(UnitComponent::stop);
                    setInputMode(InputMode.SELECT);
                });
                controlPanel.add(stopButton, 1, 0);

                atkButton = createCommandButton("Atk");
                atkButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        setInputMode(InputMode.ATTACK);
                    }
                });
                controlPanel.add(atkButton, 4, 0);

                patrolButton = createCommandButton("Patrol");
                patrolButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        setInputMode(InputMode.PATROL);
                    }
                });
                controlPanel.add(patrolButton, 3, 0);

                holdButton = createCommandButton("Hold");
                holdButton.setOnAction(event -> {
                    if (!selectedUnits.isEmpty()) {
                        forEachCommandableUnit(UnitComponent::orderHold);
                        setInputMode(InputMode.SELECT);
                    }
                });
                controlPanel.add(holdButton, 2, 0);
                //worker specific buttons
                if (unitComp.isWorker()) {
                    Button returnButton = createCommandButton("Return");
                    returnButton.setOnAction(e -> {
                        issueReturnCommand();
                    });
                    controlPanel.add(returnButton, 0, 1);

                    //one button per placeable building; pressing it enters placement mode
                    int buildColumn = 1;
                    for (BuildingStats building : BuildingDatabase.all()) {
                        Button buildButton = createCommandButton(
                                building.buttonLabel() + " " + building.mineralCost());
                        buildButton.setOnAction(e -> startPlacement(building));
                        controlPanel.add(buildButton, buildColumn++, 1);
                    }
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
                    button.setOnAction(event -> {
                        if (canCommandStructure(selectedEntity)) {
                            command.execute();
                        }
                    });

                    controlPanel.add(button, col, row);
                    //go through all 9 spots
                    col++;
                    if (col > 2){
                        col = 0;
                        row++;
                    }
                }
                //the grid only has room for 3 rows, so report anything past it
                if (row > 2){
                    System.out.println("Too many commands!");
                }
            }
        }
        else if (selectedComponent.getOwner() != null
                && PlayerManager.getHumanPlayer().getStance(selectedComponent.getOwner()) == Relation.HOSTILE) {
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

    /**
     * Cancels a construction site and refunds its full cost. Builders notice the
     * site is gone on their next tick and fall back to IDLE.
     */
    private void cancelConstruction(ConstructionComponent construction) {
        if (construction == null || construction.isComplete()) {
            return;
        }
        Entity siteEntity = construction.getEntity();
        if (siteEntity == null || !siteEntity.isActive()) {
            return;
        }
        Player owner = construction.getOwner();
        if (owner != null) {
            owner.setMinerals(owner.getMinerals() + construction.getStats().mineralCost());
        }
        System.out.println("[build] cancelled " + construction.getStats().name() + ", minerals refunded");
        siteEntity.removeFromWorld();
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

    private void issueReturnCommand() {
        forEachCommandableUnit(comp -> {
            if (comp.isWorker() && comp.hasCargo()) {
                Entity nearestBase = comp.scanAroundCC();
                if (nearestBase != null) {
                    comp.orderReturnCargo(nearestBase);
                }
            }
        });
        setInputMode(InputMode.SELECT);
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
                    //findPlayerFromID returns null for an unknown id (it does not
                    //throw), and a null owner used to NPE inside the factory
                    owner = PlayerManager.findPlayerFromID(tokens[2]);
                    if (owner == null) {
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
        //topUIBar.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0.7), CornerRadii.EMPTY, Insets.EMPTY)));

        //minerals
        HBox mineralBox = createResourceGroup(Color.CYAN, "0", "ui_icons/mineral_icon.png");
        mineralLabel = (Label) mineralBox.getChildren().get(1);

        //gas
        HBox gasBox = createResourceGroup(Color.LIMEGREEN, "0", "ui_icons/gas_icon.png");
        gasLabel = (Label) gasBox.getChildren().get(1);

        //supply
        HBox supplyBox = createResourceGroup(Color.WHITE, "0/200", "ui_icons/supply_icon.png");
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
        human.reservedSupplyProperty().addListener((obs, o, n) -> updateSupplyLabel(human));
        //manually update once at start of game
        updateSupplyLabel(human);

        //add it
        addUINode(topUIBar);
    }

    //helper to create icon-resource hbox
    private HBox createResourceGroup(Color color, String text, String imageName) {
        HBox resourceGroup = new HBox(10);
        resourceGroup.setAlignment(Pos.CENTER_LEFT);
        //tries to load in the corresponding resource icon texture
        String texturePath = "/assets/textures/"+imageName;

        var url = getClass().getResource(texturePath);

        //if file found at url
        if (url != null) {
            String validUrl = url.toExternalForm();
            Image fxImage = new Image(validUrl, 20, 20, true, true);
            Texture iconTexture = new Texture(fxImage);
            resourceGroup.getChildren().add(iconTexture);
        }
        //if can't find, use solid rectangle
        else {
            System.out.println("Image not found: " + texturePath + ", using placeholder");
            Rectangle icon = new Rectangle(20, 20, color);
            icon.setStroke(Color.GRAY);
            resourceGroup.getChildren().add(icon);
        }

        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold");

        resourceGroup.getChildren().add(label);
        return resourceGroup;
    }

    //helper to update and format supply
    private void updateSupplyLabel(Player player) {
        String reserved = player.getReservedSupply() > 0 ? " (+" + player.getReservedSupply() + " queued)" : "";
        supplyLabel.setText(player.getCurrentSupply() + " / " + player.getMaxSupply() + reserved);
    }

}
