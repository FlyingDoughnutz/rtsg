package com.example.rtsg;

import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.Structures.StructureStats;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public abstract class EntityComponent extends Component {

    public abstract IntegerProperty hpProperty();

    public abstract StringProperty nameProperty();

    public abstract int getMaxHp();
}
