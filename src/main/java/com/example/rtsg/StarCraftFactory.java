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

        UnitStats marineStats = new UnitStats("Marine", 70, 150.0, 100.0);

        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .bbox(BoundingShape.circle(20))
                .view(new Rectangle(20, 30, Color.BLUE)) //visuals, now represented as a rectangle
                .with(new UnitComponent(marineStats)) //attach to the specific behavorial code we want
                .build(); //complete
    }

    @Spawns("scv")
    public Entity newSCV(SpawnData data) {
        UnitStats scvStats = new UnitStats("SCV", 60, 150.0, 5);
        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .bbox(BoundingShape.circle(20))
                .view(new Rectangle(30, 30, Color.DARKBLUE)) //visuals, now represented as a rectangle
                .with(new UnitComponent(scvStats)) //attach to the specific behavorial code we want
                .build(); //complete
    }

    @Spawns("zergling")
    public Entity newZergling(SpawnData data) {
        UnitStats scvStats = new UnitStats("Zergling", 45, 250.0, 5);
        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .bbox(new HitBox(BoundingShape.circle(20)))
                .view(new Rectangle(30, 20, Color.PURPLE)) //visuals, now represented as a rectangle
                .with(new UnitComponent(scvStats)) //attach to the specific behavorial code we want
                .build(); //complete
    }

    @Spawns("command_centre")
    public Entity newCommandCentre(SpawnData data) {
        StructureStats ccStats = new StructureStats("Command Centre", 1600);
        return entityBuilder(data)
                .type(EntityType.STRUCTURE)
                .bbox(new HitBox(BoundingShape.box(160,160)))
                .view(new Rectangle(160, 160, Color.GRAY))
                .with(new StructureComponent(ccStats))
                .build();
    }

}
