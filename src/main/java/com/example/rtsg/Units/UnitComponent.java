package com.example.rtsg.Units;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.example.rtsg.*;
import com.example.rtsg.Structures.StructureComponent;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;

public class UnitComponent extends EntityComponent {

    private final UnitStats stats;

    private final IntegerProperty hp;
    private final StringProperty name;
    private final IntegerProperty armor;

    private UnitState state = UnitState.IDLE;

    private boolean flagPatrol = false;
    private Point2D patrolPointA;
    private Point2D patrolPointB;

    private Point2D moveTarget;
    private Entity attackTarget;
    private Point2D attackMoveDestination;

    private double attackTimer = 0.0;
    private double scanTimer = 0.0;
    private double settleTimer = 0.0;
    private Point2D lastPosition = Point2D.ZERO;
    private Point2D finalVelocity = new Point2D(0, 0);
    private boolean flagIsStuck = false;

    public UnitComponent(UnitStats stats, Player owner) {
        super(owner);
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
        this.armor = new SimpleIntegerProperty(stats.armor());
    }

    @Override
    public void onUpdate(double timePerFrame){

        //update cooldown per tick
        if (attackTimer > 0) attackTimer -= timePerFrame;
        if (scanTimer > 0) scanTimer -= timePerFrame;
        if (flagIsStuck) settleTimer += timePerFrame;

        finalVelocity = new Point2D(0, 0);

        //State Machine Logic
        switch (state) {
            case IDLE:
                if (scanTimer <= 0.0) {
                    Entity potentialTarget = scanAround();
                    if (potentialTarget != null){
                        attack(potentialTarget);
                    }
                    scanTimer += 0.5;
                }
                break;

            case MOVING:
                updateMoveLogic();
                if (moveTarget != null) calculateMovementPhysics();
                else state = UnitState.IDLE;
                break;

            case ATTACK:
                if (attackTarget == null || !attackTarget.isActive()){
                    state = UnitState.IDLE;
                    //patrol logic.
                    if (flagPatrol){}
                    //resume a-move path
                    else if (attackMoveDestination != null){
                        moveTarget = attackMoveDestination;
                        attackTarget = null;
                        state = UnitState.ATTACK_MOVE;
                        flagIsStuck = false;
                        settleTimer = 0.0;
                    }
                    else {
                        state = UnitState.IDLE;
                    }
                }
                else {
                    updateAttackLogic();
                    if (moveTarget != null) {
                        //still moving so check if stuck
                        updateStuckDetection();
                        //if stuck, give up the target
                        if(flagIsStuck && settleTimer > 0.8) {
                            moveTarget = null;
                            attackTarget = null;
                            //if we were a-moving
                            if(attackMoveDestination != null){
                                state = UnitState.ATTACK_MOVE;
                                moveTarget = attackMoveDestination;
                            }
                            else {
                                state = UnitState.IDLE;
                            }
                            flagIsStuck = false;
                            settleTimer = 0.0;
                        }
                        //if not stuck, chase
                        else {
                            calculateMovementPhysics();
                        }
                    }
                }
                break;

            case ATTACK_MOVE:
                //scan for enemies
                if (scanTimer <= 0.0) {
                    Entity potentialTarget = scanAround();
                    if (potentialTarget != null){
                        attack(potentialTarget);
                        return;
                    }
                    scanTimer = 0.2;
                }
                //move towards target point
                updateMoveLogic();
                if (moveTarget != null) calculateMovementPhysics();
                else state = UnitState.IDLE;
                attackMoveDestination = null;
                break;

            case HOLD:

                break;

            case PATROL:

                break;

            case GATHER:

                break;

            case RETURN_CARGO:

                break;

        }

        entity.translate(finalVelocity.multiply(timePerFrame));

    }

    public void moveAttack(Point2D target) {
        this.moveTarget = target;
        this.attackTarget = null;
        this.attackMoveDestination = target;
        this.flagPatrol = false;
        this.flagIsStuck = false;
        this.settleTimer = 0.0;
        setState(UnitState.ATTACK_MOVE);
    }

    //helper that scans around the current unit and finds a valid hostile target
    public Entity scanAround() {
        //find an appropriate scan radius for this unit
        //if ranged, slightly larger, if melee, set to 250
        double scanRadius = Math.max(stats.attackRange() * 1.5, 250);

        var foundEntities = getGameWorld().getEntitiesInRange(
                new Rectangle2D(entity.getCenter().getX() - scanRadius, entity.getCenter().getY() - scanRadius,
                        scanRadius * 2, scanRadius * 2));
        return foundEntities.stream()
                .filter(e -> e != entity)//has to be an entity
                .filter(e -> e.isType(EntityType.UNIT) || e.isType(EntityType.STRUCTURE))//has to be a unit or a structure
                .filter(e -> isHostile(e))//filter for hostile
                .sorted((e1, e2) -> Double.compare(e1.distance(entity), e2.distance(entity)))//sort closer targer
                .findFirst()//first found that fits all conditions
                .orElse(null);//return null if no valid target
    }

    //check if we're stuck, if so, raise a flag
    private void updateStuckDetection(){
        double distanceMoved = entity.getCenter().distance(lastPosition);
        //update last position for next frame
        lastPosition = entity.getCenter();
        if (moveTarget != null){
            //if stuck, raise flag
            if (distanceMoved < 0.25) {
                flagIsStuck = true;
            }
            //not stuck, continue
            else {
                flagIsStuck = false;
                settleTimer = 0.0;
            }
        }
    }

    //checks in-movement
    private void updateMoveLogic() {
        updateStuckDetection();
        //we have arrived at the destination
        if (moveTarget != null && entity.getCenter().distance(moveTarget) < 10) {
            moveTarget = null;
            state = UnitState.IDLE;
            return;
        }
        //stop if we're stuck
        if (flagIsStuck && settleTimer > 0.8) {
            moveTarget = null;
            state = UnitState.IDLE;
            flagIsStuck = false;
            settleTimer = 0.0;
        }
    }

    //helper for attacking related logic
    private void updateAttackLogic() {
        //attack logic
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
                moveTarget = attackTarget.getCenter();
            }
            //give up if stuck
            if (flagIsStuck && settleTimer > 0.8) {
                moveTarget = null;
                state = UnitState.IDLE;
                flagIsStuck = false;
                settleTimer = 0.0;
            }

        }
    }

    //helper for regular movement
    private void calculateMovementPhysics() {
        Point2D separation = calculateSeparation();
        Point2D moveDir = Point2D.ZERO;
        //move logic
        if (moveTarget != null) {
            Point2D moveVector = this.moveTarget.subtract(entity.getCenter());
            double distance = moveVector.magnitude();

            double currentSpeed = stats.moveSpeed();
            //if we're close and not in pursuit, slow down
            if (state == UnitState.MOVING && distance < 50) {
                currentSpeed *= (distance / 50);
            }
            moveDir = moveVector.normalize().multiply(currentSpeed);
        }
        //add this separation to the existing velocity (regular movement)
        finalVelocity = moveDir.add(separation);
        //if it's faster than intended, clamp it to the unit's speed
        if (finalVelocity.magnitude() > stats.moveSpeed()) finalVelocity = finalVelocity.normalize().multiply(stats.moveSpeed());
    }

    //check if hostile to this
    private boolean isHostile(Entity e) {

        EntityComponent targetComponent = getEntityComponent(e);
        //check for nulls
        if (targetComponent == null || targetComponent.getOwner() == null) return false;

        //find player using ID instead of getOwner
        Player actualOwner = PlayerManager.findPlayerFromID(this.getOwner().getPlayerID());

        Player actualTarget = PlayerManager.findPlayerFromID(targetComponent.getOwner().getPlayerID());

        if (actualOwner == null || actualTarget == null) return false;

        return actualOwner.getStance(actualTarget) == Relation.HOSTILE;
    }

    public void moveTo(Point2D moveTarget){
        this.moveTarget = moveTarget;
        this.attackTarget = null;
        this.attackMoveDestination =  null;
        this.flagPatrol = false;
        this.flagIsStuck = false;
        this.settleTimer = 0.0;
        setState(UnitState.MOVING);
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
        this.flagIsStuck = false;
        this.settleTimer = 0.0;
        setState(UnitState.ATTACK);
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

    public UnitState getState() {
        return state;
    }

    public void setState(UnitState state) {
        System.out.println(name.get() + " changed state to: " + state);
        this.state = state;
    }
}
