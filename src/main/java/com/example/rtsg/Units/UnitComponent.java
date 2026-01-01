package com.example.rtsg.Units;

import com.almasb.fxgl.entity.component.Component;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Point2D;

public class UnitComponent extends Component {

    private final UnitStats stats;

    private final IntegerProperty hp;
    private final StringProperty name;

    private Point2D moveTarget;

    public UnitComponent(UnitStats stats) {
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
    }

    @Override
    public void onUpdate(double timePerFrame){
        if (moveTarget != null){
            //move entity towards target
            entity.translateTowards(moveTarget, stats.moveSpeed() * timePerFrame);
            //stop when close enough to target
            if (entity.getPosition().distance(moveTarget) < 5){
                moveTarget = null;
            }
        }
    }

    public void moveTo(Point2D moveTarget){
        this.moveTarget = moveTarget;
    }

    public IntegerProperty hpProperty(){return this.hp;}
    public StringProperty nameProperty(){return this.name;}

}
