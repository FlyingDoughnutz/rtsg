package com.example.rtsg;

import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.example.rtsg.Buildings.BuildingDatabase;
import com.example.rtsg.Buildings.BuildingStats;
import com.example.rtsg.Buildings.ConstructionComponent;
import com.example.rtsg.Resources.ResourceComponent;
import com.example.rtsg.Resources.ResourceStats;
import com.example.rtsg.Structures.BaseComponent;
import com.example.rtsg.Structures.ProductionComponent;
import com.example.rtsg.Structures.StructureComponent;
import com.example.rtsg.Structures.StructureStats;
import com.example.rtsg.Units.UnitComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.example.rtsg.Units.UnitDatabase;
import com.example.rtsg.Units.UnitStats;
import javafx.geometry.Point2D;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGLForKtKt.entityBuilder;

public class StarCraftFactory implements EntityFactory {

    @Spawns("marine")
    public Entity newMarine(SpawnData data) {

        UnitStats marineStats = UnitDatabase.getStats("marine");

        Player owner = data.get("owner");

        UnitComponent marineComponent = new UnitComponent(marineStats, owner);

        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .bbox(new HitBox(new Point2D(0, 5),BoundingShape.circle(15)))
                .view(new Rectangle(20, 30, owner.getPlayerColor())) //visuals, now represented as a rectangle
                .with(marineComponent) //attach to the specific behavorial code we want
                .with(new HealthBarComponent(marineComponent)) //attach health bar
                .build(); //complete
    }

    @Spawns("scv")
    public Entity newSCV(SpawnData data) {
        UnitStats scvStats = UnitDatabase.getStats("scv");
        Player owner = data.get("owner");
        UnitComponent scvComponent = new UnitComponent(scvStats, owner);
        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .bbox(new HitBox(new Point2D(5, 0),BoundingShape.circle(15)))
                .view(new Rectangle(30, 30, owner.getPlayerColor())) //visuals, now represented as a rectangle
                .with(scvComponent) //attach to the specific behavorial code we want
                .with(new HealthBarComponent(scvComponent))
                .build(); //complete
    }

    @Spawns("zergling")
    public Entity newZergling(SpawnData data) {
        UnitStats zerglingStats = UnitDatabase.getStats("zergling");
        Player owner = data.get("owner");
        UnitComponent zerglingComponent = new UnitComponent(zerglingStats, owner);
        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .bbox(new HitBox(BoundingShape.circle(20)))
                .view(new Rectangle(30, 20, owner.getPlayerColor())) //visuals, now represented as a rectangle
                .with(zerglingComponent) //attach to the specific behavorial code we want
                .with(new HealthBarComponent(zerglingComponent))
                .build(); //complete
    }

    @Spawns("command_centre")
    public Entity newCommandCentre(SpawnData data) {
        BuildingStats building = BuildingDatabase.get("command_centre");
        Player owner = data.get("owner");
        StructureComponent ccComponent = new StructureComponent(structureStats(building), owner);
        ProductionComponent prodComponent = new ProductionComponent(owner);
        ccComponent.addCommand("Train SCV", () -> prodComponent.buildUnit("scv"));
        ccComponent.addCommand("Lift Off", () -> {
            System.out.println("Lifting off...");
            // Logic to switch state to FLYING
        });
        return structureEntity(data, building, owner, ccComponent, prodComponent)
                .with(new BaseComponent())
                .build();
    }

    @Spawns("barracks")
    public Entity newBarracks(SpawnData data) {
        BuildingStats building = BuildingDatabase.get("barracks");
        Player owner = data.get("owner");
        StructureComponent barracksComponent = new StructureComponent(structureStats(building), owner);
        ProductionComponent prodComponent = new ProductionComponent(owner);
        barracksComponent.addCommand("Train Marine", () -> prodComponent.buildUnit("marine"));
        return structureEntity(data, building, owner, barracksComponent, prodComponent)
                .build();
    }

    @Spawns("supply_depot")
    public Entity newSupplyDepot(SpawnData data) {
        BuildingStats building = BuildingDatabase.get("supply_depot");
        Player owner = data.get("owner");
        StructureComponent depotComponent = new StructureComponent(structureStats(building), owner);
        return structureEntity(data, building, owner, depotComponent)
                .build();
    }

    /**
     * A tile-aligned construction site. Workers walk to it and add build time;
     * when it completes the site replaces itself with the real building.
     */
    @Spawns("construction_site")
    public Entity newConstructionSite(SpawnData data) {
        BuildingStats building = data.get("building");
        Player owner = data.get("owner");
        int tileX = data.get("tileX");
        int tileY = data.get("tileY");
        ConstructionComponent construction = new ConstructionComponent(building, owner, tileX, tileY);

        double width = building.pixelWidth();
        double height = building.pixelHeight();
        Color tint = owner == null ? Color.GRAY : owner.getPlayerColor();

        Rectangle area = new Rectangle(width, height,
                Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.28));
        area.setStroke(Color.WHITE);
        area.setStrokeWidth(1.5);
        area.getStrokeDashArray().addAll(6.0, 4.0);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.progressProperty().bind(construction.progressProperty());
        progressBar.setPrefWidth(Math.max(24, width - 20));
        progressBar.setPrefHeight(8);

        StackPane view = new StackPane(area, progressBar);
        view.setPickOnBounds(false);

        return entityBuilder(data)
                .type(EntityType.CONSTRUCTION)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(view)
                .with(construction)
                .build();
    }

    @Spawns("mineral_large")
    public Entity newMineralLarge(SpawnData data) {
        ResourceStats mineralLargeStats = new ResourceStats("Large Mineral Field", 1800, 0);
        Player owner = data.get("owner");
        ResourceComponent mineralLargeComponent = new ResourceComponent(mineralLargeStats, owner);
        return entityBuilder(data)
                .type(EntityType.RESOURCE)
                .viewWithBBox(new Rectangle(60, 30, owner.getPlayerColor()))
                .with(mineralLargeComponent)
                .build();
    }

    @Spawns("mineral_small")
    public Entity newMineralSmall(SpawnData data) {
        ResourceStats mineralSmallStats = new ResourceStats("Small Mineral Field", 900, 0);
        Player owner = data.get("owner");
        ResourceComponent mineralSmallComponent = new ResourceComponent(mineralSmallStats, owner);
        return entityBuilder(data)
                .type(EntityType.RESOURCE)
                .viewWithBBox(new Rectangle(60, 30, owner.getPlayerColor()))
                .with(mineralSmallComponent)
                .build();
    }

    private static StructureStats structureStats(BuildingStats building) {
        return new StructureStats(building.name(), building.maxHp(), building.armor(), building.supplyProvided());
    }

    /**
     * Shared builder for finished buildings so the tile footprint from
     * {@link BuildingDatabase} is always what the entity's view and hitbox use.
     */
    private com.almasb.fxgl.dsl.EntityBuilder structureEntity(SpawnData data,
                                                              BuildingStats building,
                                                              Player owner,
                                                              StructureComponent structure,
                                                              com.almasb.fxgl.entity.component.Component... extras) {
        var builder = entityBuilder(data)
                .type(EntityType.STRUCTURE)
                .bbox(new HitBox(BoundingShape.box(building.pixelWidth(), building.pixelHeight())))
                .view(new Rectangle(building.pixelWidth(), building.pixelHeight(), owner.getPlayerColor()))
                .with(structure)
                .with(new HealthBarComponent(structure));
        for (var extra : extras) {
            builder = builder.with(extra);
        }
        return builder;
    }
}
