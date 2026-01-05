package com.example.rtsg.Resources;

import com.example.rtsg.EntityComponent;
import com.example.rtsg.Player;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ResourceComponent extends EntityComponent {

    private final ResourceStats stats;

    private final StringProperty name;
    private final IntegerProperty currResource; //effectively hp
    private final IntegerProperty currType; //type, 0=mineral, 1=gas
    private final IntegerProperty armor; //to prevent null pointer exceptions

    public ResourceComponent(ResourceStats stats, Player owner) {
        super(owner);
        this.stats = stats;
        this.name = new SimpleStringProperty(stats.name());
        this.currResource = new SimpleIntegerProperty(stats.resourceAmount());
        this.currType = new SimpleIntegerProperty(stats.resourceType());
        this.armor = new SimpleIntegerProperty(0);
    }

    @Override
    public IntegerProperty hpProperty() {
        return this.currResource;
    }

    @Override
    public StringProperty nameProperty() {
        return this.name;
    }

    @Override
    public IntegerProperty armorProperty() {
        return this.armor;
    }

    @Override
    public int getMaxHp() {
        return stats.resourceAmount();
    }

    public int getResourceType() {
        return stats.resourceType();
    }
}
