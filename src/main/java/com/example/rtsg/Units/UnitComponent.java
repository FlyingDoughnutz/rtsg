package com.example.rtsg.Units;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.EntityComponent;
import com.example.rtsg.EntityType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Circle;

public class UnitComponent extends EntityComponent {

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

        Point2D separation = calculateSeparation();
        Point2D moveDir = Point2D.ZERO;

        if (moveTarget != null) {
            Point2D moveVector = this.moveTarget.subtract(entity.getPosition());
            double distance = moveVector.magnitude();

            if (distance < 5) {
                moveTarget = null;
            } else {
                double currentSpeed = stats.moveSpeed();
                if (distance < 50) {
                    currentSpeed *= (distance / 50);
                }
                moveDir = moveVector.normalize().multiply(currentSpeed);
            }
        }

        Point2D finalVelocity = moveDir.add(separation);

        if (finalVelocity.magnitude() < 1.0) {
            return;
        }

        // This prevents the forces from stacking to create "super speed"
        // and ensures separation has "room" to work within the speed limit.
        if (finalVelocity.magnitude() > stats.moveSpeed()) {
            finalVelocity = finalVelocity.normalize().multiply(stats.moveSpeed());
        }

        entity.translate(finalVelocity.multiply(timePerFrame));
    }

    public void moveTo(Point2D moveTarget){
        this.moveTarget = moveTarget;
    }

    @Override
    public IntegerProperty hpProperty(){return this.hp;}
    @Override
    public StringProperty nameProperty(){return this.name;}

    //helper method to calculate the amount to bump each unit by when colliding
    private Point2D calculateSeparation(){
        Point2D separationVector = Point2D.ZERO;
        double spaceToKeep = 35.0;

        Rectangle2D detectionArea = new Rectangle2D(
                entity.getX() - spaceToKeep,
                entity.getY() - spaceToKeep,
                spaceToKeep * 2,
                spaceToKeep * 2
        );

        var otherUnits = FXGL.getGameWorld().getEntitiesInRange(detectionArea);

        for (Entity other: otherUnits){
            if (other == entity) continue;

            double dist = entity.distance(other);

            if (dist < spaceToKeep && dist > 0){
                Point2D pushAway= entity.getPosition().subtract(other.getPosition());
                double strength = (spaceToKeep - dist) / spaceToKeep;
                separationVector = separationVector.add(pushAway.normalize().multiply(stats.moveSpeed() * strength));
            }
        }

        return separationVector;
    }

}
