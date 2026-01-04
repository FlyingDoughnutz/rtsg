package com.example.rtsg.Structures;

import com.example.rtsg.EntityComponent;
import com.example.rtsg.Player;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StructureComponent extends EntityComponent {

    private final StructureStats stats;

    private final IntegerProperty hp;
    private final StringProperty name;
    private final IntegerProperty armor;

    public StructureComponent(StructureStats stats, Player owner) {
        super(owner);
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
        this.armor = new SimpleIntegerProperty(stats.armor());
    }

    @Override
    public IntegerProperty hpProperty(){return this.hp;}
    @Override
    public StringProperty nameProperty(){return this.name;}
    @Override
    public IntegerProperty armorProperty() {return this.armor;}

    @Override
    public int getMaxHp() {return stats.maxHp();}

}
