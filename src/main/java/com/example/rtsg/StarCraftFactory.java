package com.example.rtsg;

import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
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
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.spawn;
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
                .bbox(new HitBox(new Point2D(5, 0), BoundingShape.circle(15)))
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
        StructureStats ccStats = new StructureStats("Command Centre", 2500, 1, 15);
        Player owner = data.get("owner");
        StructureComponent ccComponent = new StructureComponent(ccStats, owner);
        ProductionComponent prodComponent = new ProductionComponent(owner);
        ccComponent.addCommand("Train SCV", () -> {
            System.out.println("Queued SCV");
            prodComponent.buildUnit("scv");
        });
        ccComponent.addCommand("Lift Off", () -> {
            System.out.println("Lifting off...");
            // Logic to switch state to FLYING
        });
        return entityBuilder(data)
                .type(EntityType.STRUCTURE)
                .bbox(new HitBox(BoundingShape.box(150,150)))
                .view(new Rectangle(150, 150, owner.getPlayerColor()))
                .with(ccComponent)
                .with(new HealthBarComponent(ccComponent))
                .with(new BaseComponent())
                .with(prodComponent)
                .build();
    }

    @Spawns("barracks")
    public Entity newBarracks(SpawnData data) {
        StructureStats barracksStats = new StructureStats("Barracks", 1000, 1, 0);
        Player owner = data.get("owner");
        StructureComponent barracksComponent = new StructureComponent(barracksStats, owner);
        barracksComponent.addCommand("Marines", () -> {
            //
        });
        barracksComponent.addCommand("Marauders", () -> {
           //hhq
        });
        return entityBuilder(data)
                .type(EntityType.STRUCTURE)
                .bbox(new HitBox(BoundingShape.box(90,90)))
                .view(new Rectangle(90, 90, owner.getPlayerColor()))
                .with(barracksComponent)
                .with(new HealthBarComponent(barracksComponent))
                .with(new ProductionComponent(owner))
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


}
