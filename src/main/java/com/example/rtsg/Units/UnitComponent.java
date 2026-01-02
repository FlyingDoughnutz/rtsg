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
    @Override
    public int getMaxHp() {return stats.maxHp();}

    //helper method to calculate the amount to bump each unit by when colliding
    private Point2D calculateSeparation(){
        Point2D separationVector = Point2D.ZERO;

        double unitRadius = entity.getWidth() / 2.0;
        double searchDist = unitRadius * 4.0 + 50.0;

        Rectangle2D detectionArea = new Rectangle2D(
                entity.getX() - searchDist,
                entity.getY() - searchDist,
                searchDist * 2,
                searchDist * 2
        );

        var nearbyUnits = FXGL.getGameWorld().getEntitiesInRange(detectionArea);

        for (Entity other: nearbyUnits){
            if (other == entity) continue;

            //Rectangle Structure collision
            if (other.isType(EntityType.STRUCTURE)||other.isType(EntityType.RESOURCE)){
                //get the exact bounds of the structure/resource
                double bMinX = other.getX();
                double bMinY = other.getY();
                double bMaxX = bMinX + other.getWidth();
                double bMaxY = bMinY + other.getHeight();
                //find the closest point on the building to the center of this entity
                Point2D myCenter = entity.getCenter();
                double closestX = Math.max(bMinX, Math.min(myCenter.getX(), bMaxX));
                double closestY = Math.max(bMinY, Math.min(myCenter.getY(), bMaxY));
                Point2D closestPoint = new Point2D(closestX, closestY);
                //calculate distance to this closest point from center of this entity
                double distToWall = myCenter.distance(closestPoint);
                //Unit centre should be at least its own radius away from the wall
                double overlap = unitRadius + 2.0 - distToWall;
                //if in the building, push really hard outwards.
                if (overlap > 0){
                    Point2D pushDir = myCenter.subtract(closestPoint).normalize();
                    if (pushDir.magnitude() == 0) pushDir = new Point2D(1,0);

                    double maxPush = 5.0;
                    double pushForceScale = Math.min(overlap / 10.0, 1.0);
                    separationVector = separationVector.add(pushDir.multiply(stats.moveSpeed() * maxPush * pushForceScale));
                }

            }

            //Circular Unit collision
            else if (other.isType(EntityType.UNIT)){
                //gets the size of the other unit to collide with
                double otherRadius = other.getWidth() / 2.0;
                //space between the 2 units should be their conbined radius
                double requiredSpace = unitRadius + otherRadius;
                //distance from this center to the other's center
                double dist = entity.getCenter().distance(other.getCenter());
                //if they're not overlapping but too close together
                if (dist < requiredSpace && dist > 0){
                    //calculate the vector to push away by
                    Point2D pushAway = entity.getCenter().subtract(other.getCenter());
                    //strength is stronger the deeper the overlap is
                    double strength = (requiredSpace - dist) / requiredSpace;
                    //the vector to separate by is normalized for direction, then multipled by given strength and unit speed then added
                    separationVector = separationVector.add(pushAway.normalize().multiply(stats.moveSpeed() * strength));
                }
            }

        }

        return separationVector;
    }

}
