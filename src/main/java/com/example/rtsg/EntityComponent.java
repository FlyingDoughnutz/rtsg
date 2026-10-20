package com.example.rtsg;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.Structures.StructureStats;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public abstract class EntityComponent extends Component {

    private Player owner;

    public EntityComponent(Player owner) {
        this.owner = owner;
    }

    private boolean isDead = false;

    @Override
    public void onAdded() {
        hpProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() <= 0) onDeath();
        });
    }

    protected void onDeath() {
        //hp can be written more than once, so make death idempotent
        if (isDead) return;
        isDead = true;
        entity.removeFromWorld();
        System.out.println(nameProperty().get() + " has been destroyed");
    }

    public Player getOwner() {
        return owner;
    }

    public abstract IntegerProperty hpProperty();

    public abstract StringProperty nameProperty();

    public abstract IntegerProperty armorProperty();

    public abstract int getMaxHp();
}
