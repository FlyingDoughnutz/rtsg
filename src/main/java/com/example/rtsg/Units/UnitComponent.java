package com.example.rtsg.Units;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.EntityComponent;
import com.example.rtsg.EntityType;
import com.example.rtsg.Owner;
import com.example.rtsg.Structures.StructureComponent;
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
    private final IntegerProperty armor;

    private Point2D moveTarget;
    private Entity attackTarget;
    private double attackTimer = 0.0;

    public UnitComponent(UnitStats stats, Owner owner) {
        super(owner);
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
        this.armor = new SimpleIntegerProperty(stats.armor());
    }

    @Override
    public void onUpdate(double timePerFrame){

        //collision and separation
        Point2D separation = calculateSeparation();
        Point2D moveDir = Point2D.ZERO;

        //attack logic
        //update cooldown per tick
        if (attackTimer > 0) attackTimer -= timePerFrame;

        if (attackTarget != null){

            EntityComponent targetStats = getEntityComponent(attackTarget);
            //first check if target is still there
            if (!attackTarget.isActive() || targetStats.hpProperty().get() <= 0 || targetStats == null) {
                attackTarget = null;
                moveTarget = null;
                return;
            }
            //get distance to target
            double distToTarget = entity.getCenter().distance(attackTarget.getCenter());
            //calculate physical distance accounting for entity model size
            double combinedRadius = (entity.getWidth() / 2.0) + (attackTarget.getWidth() / 2.0);
            //calculate actual attack range, taking in consideration of unit size
            double effectiveRange = stats.attackRange() + combinedRadius;

            //if in range, stop and attack
            if (distToTarget < effectiveRange) {
                moveTarget = null;
                if (attackTimer <= 0) {
                    performAttack();
                    attackTimer = stats.attackSpeed();
                }
            }
            //if out of range, move to chase target
            else {
                // Calculate the position that puts our CENTER on their CENTER
                // (TargetPos + TargetHalfSize) - MyHalfSize
                double targetX = attackTarget.getX() + attackTarget.getWidth() / 2.0 - entity.getWidth() / 2.0;
                double targetY = attackTarget.getY() + attackTarget.getHeight() / 2.0 - entity.getHeight() / 2.0;

                moveTarget = new Point2D(targetX, targetY);
            }

        }

        //move logic

        if (moveTarget != null) {
            Point2D moveVector = this.moveTarget.subtract(entity.getPosition());
            double distance = moveVector.magnitude();

            if (attackTarget == null && distance < 5) {
                moveTarget = null;
            } else {
                double currentSpeed = stats.moveSpeed();

                if (attackTarget == null && distance < 50) {
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
        this.attackTarget = null;
    }

    @Override
    public IntegerProperty hpProperty(){return this.hp;}
    @Override
    public StringProperty nameProperty(){return this.name;}

    @Override
    public IntegerProperty armorProperty() {return this.armor;}

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

    //performs the attack
    private void performAttack() {
        System.out.println(stats.name() + "attacked" + attackTarget.getType());

        EntityComponent targetStats = getEntityComponent(attackTarget);

        if  (targetStats != null) {
            int currentHp = targetStats.hpProperty().get();
            int damage = (int) stats.attackDamage();
            int armor = targetStats.armorProperty() != null ? targetStats.armorProperty().get() : 0;
            targetStats.hpProperty().set(Math.max(0, currentHp - (damage - armor)));
        }

    }

    //assigns target for attack
    public void attack(Entity target){
        this.attackTarget = target;
        this.moveTarget = null;
    }

    //helper to
    private EntityComponent getEntityComponent(Entity entity) {

        if (entity.hasComponent(UnitComponent.class)) {
            return entity.getComponent(UnitComponent.class);
        }
        else if (entity.hasComponent(StructureComponent.class)) {
            return entity.getComponent(StructureComponent.class);
        }
        return null;
    }

}
