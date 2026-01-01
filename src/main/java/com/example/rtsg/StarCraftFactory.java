package com.example.rtsg;

import com.example.rtsg.Units.UnitComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.example.rtsg.Units.UnitStats;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGLForKtKt.entityBuilder;

public class StarCraftFactory implements EntityFactory {

    @Spawns("marine")
    public Entity newMarine(SpawnData data) {

        UnitStats marineStats = new UnitStats("Marine", 70, 150.0, 100.0);

        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .viewWithBBox(new Rectangle(20, 30, Color.BLUE)) //visuals, now represented as a rectangle
                .with(new UnitComponent(marineStats)) //attach to the specific behavorial code we want
                .build(); //complete
    }

    @Spawns("scv")
    public Entity newSCV(SpawnData data) {
        UnitStats scvStats = new UnitStats("SCV", 60, 150.0, 5);
        return entityBuilder(data)
                .type(EntityType.UNIT) //declare the type we made in the enums
                .viewWithBBox(new Rectangle(30, 30, Color.DARKBLUE)) //visuals, now represented as a rectangle
                .with(new UnitComponent(scvStats)) //attach to the specific behavorial code we want
                .build(); //complete
    }

}
