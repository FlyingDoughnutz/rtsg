package com.example.rtsg;

import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.example.rtsg.Structures.StructureComponent;
import com.example.rtsg.Structures.StructureStats;
import com.example.rtsg.Units.UnitComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.example.rtsg.Units.UnitStats;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGLForKtKt.entityBuilder;

public class StarCraftFactory implements EntityFactory {

    @Spawns("marine")
    public Entity newMarine(SpawnData data) {

        UnitStats marineStats = new UnitStats("Marine", 70, 150, 150, 7, 0.5, 1);

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
        UnitStats scvStats = new UnitStats("SCV", 60, 150, 15, 5, 1, 0);
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
        UnitStats zerglingStats = new UnitStats("Zergling", 45, 250, 15, 6, 0.8, 0);
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
        StructureStats ccStats = new StructureStats("Command Centre", 1600, 0);
        Player owner = data.get("owner");
        StructureComponent ccComponent = new StructureComponent(ccStats, owner);
        return entityBuilder(data)
                .type(EntityType.STRUCTURE)
                .bbox(new HitBox(BoundingShape.box(150,150)))
                .view(new Rectangle(150, 150, owner.getPlayerColor()))
                .with(ccComponent)
                .with(new HealthBarComponent(ccComponent))
                .build();
    }

}
