package com.example.rtsg;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.Structures.StructureStats;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public abstract class EntityComponent extends Component {

    private Owner owner;

    public EntityComponent(Owner owner) {
        this.owner = owner;
    }

    @Override
    public void onAdded() {
        hpProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() <= 0) onDeath();
        });
    }

    protected void onDeath() {
        entity.removeFromWorld();
        System.out.println(nameProperty().get() + " has been destroyed");
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {}

    public abstract IntegerProperty hpProperty();

    public abstract StringProperty nameProperty();

    public abstract IntegerProperty armorProperty();

    public abstract int getMaxHp();
}
