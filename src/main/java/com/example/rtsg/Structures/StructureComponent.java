package com.example.rtsg.Structures;

import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.EntityComponent;
import com.example.rtsg.Units.UnitStats;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StructureComponent extends EntityComponent {

    private final StructureStats stats;

    private final IntegerProperty hp;
    private final StringProperty name;

    public StructureComponent(StructureStats stats) {
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
    }

    @Override
    public IntegerProperty hpProperty(){return this.hp;}
    @Override
    public StringProperty nameProperty(){return this.name;}

}
