package com.example.rtsg.Units;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.example.rtsg.*;
import com.example.rtsg.Resources.ResourceComponent;
import com.example.rtsg.Structures.BaseComponent;
import com.example.rtsg.Structures.StructureComponent;
import javafx.beans.property.*;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;

public class UnitComponent extends EntityComponent {

    private final UnitStats stats;

    private final IntegerProperty hp;
    private final StringProperty name;
    private final IntegerProperty armor;
    private final BooleanProperty isWorker;

    private UnitState state = UnitState.IDLE;

    private boolean flagPatrol = false;
    private boolean flagHold = false;
    private Point2D patrolPointA;
    private Point2D patrolPointB;
    private Point2D currentPatrolTarget;

    private Point2D moveTarget;
    private Entity attackTarget;
    private Point2D attackMoveDestination;

    private double attackTimer = 0.0;
    private double scanAttackTimer = 0.0;
    private double settleTimer = 0.0;
    private Point2D lastPosition = Point2D.ZERO;
    private Point2D finalVelocity = new Point2D(0, 0);
    private boolean flagIsStuck = false;

    private final int COLLECTION_RATE = 2;
    private final int COLLECTION_CAPACITY = 8;
    private double miningTimer = 0.0;
    private final int MINERAL_SCAN_RANGE = 200;
    private double scanMiningTimer = 0.0;
    private double scanCCTimer = 0.0;
    private Entity miningTarget;
    private Entity returnTarget;
    private int resourceCarried = 0;

    public UnitComponent(UnitStats stats, Player owner) {
        super(owner);
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
        this.armor = new SimpleIntegerProperty(stats.armor());
        this.isWorker = new SimpleBooleanProperty(stats.isWorker());
    }

    @Override
    public void onUpdate(double timePerFrame){

        //update cooldown per tick
        if (attackTimer > 0) attackTimer -= timePerFrame;
        if (scanAttackTimer > 0) scanAttackTimer -= timePerFrame;
        if (this.stats.isWorker() && scanMiningTimer > 0) scanMiningTimer -= timePerFrame;
        if (this.stats.isWorker() && scanCCTimer > 0) scanCCTimer -= timePerFrame;
        if (this.stats.isWorker() && miningTimer > 0) miningTimer -= timePerFrame;
        if (flagIsStuck) settleTimer += timePerFrame;

        finalVelocity = new Point2D(0, 0);

        //State Machine Logic
        switch (state) {
            case IDLE:
                if (!this.stats.isWorker()) {
                    if (scanAttackTimer <= 0.0) {
                        Entity potentialTarget = scanAroundAttack();
                        if (potentialTarget != null) {
                            attack(potentialTarget);
                        }
                        scanAttackTimer += 0.5;
                    }
                }
                //if worker, if not gathering, find another mineral patch in the vicinity
                // else {
                //    if (miningTarget == null) {
                //        if (scanMiningTimer <= 0.0) {
                //            Entity potentialMineTarget = scanAroundMine();
                //            if (potentialMineTarget != null) {
                //                mine(potentialMineTarget);
                //            }
                //        }
                //        scanMiningTimer += 0.5;
                //    }
                //}
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
                    if (flagPatrol){
                        state = UnitState.PATROL;
                        moveTarget = currentPatrolTarget;
                        attackTarget = null;
                    }
                    //hold logic
                    else if (flagHold){
                        state = UnitState.HOLD;
                        moveTarget = null;
                        attackTarget = null;
                    }
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
                    //move to chase only if not in hold mode
                    if (!flagHold && moveTarget != null) {
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
                            else if (flagPatrol){
                                state = UnitState.PATROL;
                            }
                            else if(flagHold){
                                state = UnitState.HOLD;
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
                    //force stop chase if in hold mode
                    else if (flagHold && moveTarget != null) {
                        moveTarget = null;
                    }
                }
                break;

            case ATTACK_MOVE:
                //scan for enemies
                if (scanAttackTimer <= 0.0) {
                    Entity potentialTarget = scanAroundAttack();
                    if (potentialTarget != null){
                        attack(potentialTarget);
                        return;
                    }
                    scanAttackTimer = 0.2;
                }
                //move towards target point
                updateMoveLogic();
                if (moveTarget != null) calculateMovementPhysics();
                else state = UnitState.IDLE;
                attackMoveDestination = null;
                break;

            case HOLD:

                moveTarget = null;
                //scan around for enemies
                if (scanAttackTimer <= 0.0) {
                    Entity potentialTarget = scanAroundAttack();
                    if (potentialTarget != null){
                        //calculate distance between this and the found target
                        double distToTarget = entity.getCenter().distance(potentialTarget.getCenter());
                        double combinedDistance = (entity.getWidth() / 2.0) + (potentialTarget.getWidth() / 2.0);
                        //check if this can actually reach the target w/o moving
                        if (distToTarget <= stats.attackRange() + combinedDistance) {
                            attack(potentialTarget);
                            this.flagHold = true;
                        }
                    }
                    scanAttackTimer = 0.5;
                }
                break;

            case PATROL:
                //scan for enemies has highest priority
                if (!this.stats.isWorker()) {
                    if (scanAttackTimer <= 0.0) {
                        Entity potentialTarget = scanAroundAttack();
                        if (potentialTarget != null) {
                            attack(potentialTarget);
                            this.flagPatrol = true;
                            return;
                        }
                        //a bit faster since patrols need to be en guarde
                        scanAttackTimer = 0.2;
                    }
                }

                //then handle movement
                if (moveTarget == null) {
                    //if we finished killing a unit, resume patrol
                    moveTarget = currentPatrolTarget;
                }
                //calculate distance to next patrol point
                double distToPoint = entity.getCenter().distance(moveTarget);
                //if close enough, switch target to the other patrol point
                if (distToPoint < 10) {
                    if (moveTarget == patrolPointA) {
                        currentPatrolTarget = patrolPointB;
                    }
                    else{
                        currentPatrolTarget = patrolPointA;
                    }
                    moveTarget = currentPatrolTarget;
                }

                calculateMovementPhysics();
                break;

            case GATHER:
                //check if in need of a valid minable patch
                boolean needNewTarget = false;
                //check if the resource is still there first
                if (miningTarget == null || !miningTarget.isActive()) {
                    needNewTarget = true;
                }
                else {
                    var resComp = miningTarget.getComponentOptional(ResourceComponent.class).orElse(null);
                    //check if it exists or has minerals left
                    if (resComp == null || resComp.hpProperty().get() <= 0) {
                        needNewTarget = true;
                    }
                }
                //if needed, find a new patch within the case
                if (needNewTarget) {
                    miningTarget = scanAroundMine();
                    //found mineral, stop movement to go to new patch
                    if (miningTarget != null) {
                        moveTarget = null;
                    }
                    //no minerals found
                    else {
                        state = UnitState.IDLE;
                    }
                    return;
                }
                //move towards mineral
                if (moveTarget == null) moveTarget = miningTarget.getCenter();
                //calculate distance to mineral patch
                double distToMine = entity.getCenter().distance(miningTarget.getCenter());
                //get effective range needed for mining, +5 for buffer
                double combinedDistance = (entity.getWidth() / 2.0) + (miningTarget.getWidth() / 2.0) + 5;
                //if close enough, mine
                if (distToMine < combinedDistance) {
                    //stop moving before mining
                    moveTarget = null;
                    finalVelocity = Point2D.ZERO;
                    if (miningTimer <= 0.0) {
                        //mine
                        performMining();
                        miningTimer = 1.0;
                        //return cargo
                        if (resourceCarried >= COLLECTION_CAPACITY) {
                            //try to find cc to return to if there's none
                            if (returnTarget == null) {
                                returnTarget = scanAroundCC();
                            }
                            //found cc, return
                            if (returnTarget != null) {
                                state = UnitState.RETURN_CARGO;
                                moveTarget = null;
                            }
                            //no cc found
                            else {
                                System.out.println("No command centre found!");
                                state = UnitState.IDLE;
                            }
                        }
                    }
                }
                //if we're still too far from resource, keep moving
                else {
                    calculateMovementPhysics();
                }

                break;

            case RETURN_CARGO:
                //check for return cc
                if (returnTarget == null || !returnTarget.isActive()) {
                    returnTarget = scanAroundCC();
                    //can't find cc -> idle
                    if (returnTarget == null) {
                        state = UnitState.IDLE;
                        return;
                    }
                }
                //move towards cc
                if (moveTarget == null) moveTarget = returnTarget.getCenter();
                //calculate distance to cc
                double distToCC = entity.getCenter().distance(returnTarget.getCenter());
                //drop-off range with +5 buffer
                double combinedDistanceCC = (entity.getWidth() / 2.0) + (returnTarget.getWidth() / 2.0) + 5;
                //if close enough, return
                if (distToCC < combinedDistanceCC) {
                    returnResources();
                    //return to mineral again
                    if (miningTarget == null || !miningTarget.isActive()) {
                        miningTarget = scanAroundMine();
                    }
                    //if there's a resource patch found, return to mine
                    if (miningTarget != null) {
                        moveTarget = null;
                        state = UnitState.GATHER;
                    }
                    //otherwise go into idle
                    else{
                        state = UnitState.IDLE;
                    }
                }
                //not close enough -> keep moving
                else {
                    calculateMovementPhysics();
                }
                break;

        }

        entity.translate(finalVelocity.multiply(timePerFrame));

    }

    public boolean isWorker() {
        return stats.isWorker();
    }

    public boolean hasCargo() {
        return resourceCarried > 0;
    }

    private void returnResources() {
        this.getOwner().setMinerals(this.getOwner().getMinerals() + resourceCarried);
        resourceCarried = 0;
    }

    public void orderReturnCargo(Entity baseTarget) {
        this.returnTarget = baseTarget;
        this.moveTarget = null;
        this.attackTarget = null;
        this.miningTarget = null;
        this.flagPatrol = false;
        this.flagHold = false;
        this.flagIsStuck = false;
        setState(UnitState.RETURN_CARGO);
    }

    public void orderPatrol(Point2D patrolTarget){
        this.patrolPointA = entity.getCenter();
        this.patrolPointB = patrolTarget;
        this.currentPatrolTarget = patrolPointB;
        this.moveTarget = currentPatrolTarget;
        this.attackTarget = null;
        this.attackMoveDestination = null;
        this.flagPatrol = true;
        this.flagHold = false;
        this.flagIsStuck = false;
        setState(UnitState.PATROL);
    }

    public void orderHold(){
        this.moveTarget = null;
        this.attackTarget = null;
        this.attackMoveDestination =  null;
        this.flagPatrol = false;
        this.flagHold = true;
        this.flagIsStuck = false;
        setState(UnitState.HOLD);
    }

    public void moveAttack(Point2D target) {
        this.moveTarget = target;
        this.attackTarget = null;
        this.attackMoveDestination = target;
        this.flagPatrol = false;
        this.flagIsStuck = false;
        this.flagHold = false;
        this.settleTimer = 0.0;
        setState(UnitState.ATTACK_MOVE);
    }

    //helper that scans around the current unit and finds a valid hostile target
    public Entity scanAroundAttack() {
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

    //helper that scans around the current unit and finds a valid mineral patch
    public Entity scanAroundMine() {

        var foundEntities = getGameWorld().getEntitiesInRange(
                new Rectangle2D(entity.getCenter().getX() - MINERAL_SCAN_RANGE, entity.getCenter().getY() - MINERAL_SCAN_RANGE,
                        MINERAL_SCAN_RANGE * 2, MINERAL_SCAN_RANGE * 2));
        return foundEntities.stream()
                .filter(e -> e != entity)//has to be an entity
                .filter(e -> e.isType(EntityType.RESOURCE))//has to be a resource
                .filter(e -> e.hasComponent(ResourceComponent.class))//filter resource with logic
                .filter(e -> e.getComponent(ResourceComponent.class).getResourceType() == 0)//filters for minerals
                .filter(e -> e.getComponent(ResourceComponent.class).hpProperty().get() > 0)
                .sorted((e1, e2) -> Double.compare(e1.distance(entity), e2.distance(entity)))//sort closer targer
                .findFirst()//first found that fits all conditions
                .orElse(null);//return null if no valid target
    }

    //finds a nearby cc
    public Entity scanAroundCC() {
        var foundEntities = getGameWorld().getEntitiesInRange(
                new Rectangle2D(entity.getCenter().getX() - MINERAL_SCAN_RANGE, entity.getCenter().getY() - MINERAL_SCAN_RANGE,
                        MINERAL_SCAN_RANGE * 2, MINERAL_SCAN_RANGE * 2));
        return foundEntities.stream()
                .filter(e -> e != entity)//has to be an entity
                .filter(e -> e.isType(EntityType.STRUCTURE))//has to be a resource
                .filter(e -> e.hasComponent(StructureComponent.class))//filter structure with logic
                .filter(e -> e.hasComponent(BaseComponent.class))//filters for bases
                .sorted((e1, e2) -> Double.compare(e1.distance(entity), e2.distance(entity)))//sort closer targer
                .findFirst()//first found that fits all conditions
                .orElse(null);//return null if no valid target
    }

    private void performMining(){
        if(miningTarget != null && miningTarget.hasComponent(ResourceComponent.class)){

            EntityComponent targetStats = getEntityComponent(miningTarget);

            if  (targetStats != null) {
                int currentResource = targetStats.hpProperty().get();
                int collected_amount = Math.min(COLLECTION_RATE, currentResource);
                //calculate space left to clamp to collection capacity
                int spaceLeft = COLLECTION_CAPACITY - resourceCarried;
                collected_amount = Math.min(collected_amount, spaceLeft);
                //add only if actually collected something
                if(collected_amount > 0){
                    targetStats.hpProperty().set(currentResource - collected_amount);
                    resourceCarried += collected_amount;
                    return;
                }
            }
        }
        System.out.println("Not a real resource mined!");
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
        this.flagHold = false;
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

    public void mine(Entity target){
        this.miningTarget = target;
        this.moveTarget = null;
        this.flagIsStuck = false;
        this.settleTimer = 0.0;
        setState(UnitState.GATHER);
    }

    //helper to
    private EntityComponent getEntityComponent(Entity entity) {

        if (entity.hasComponent(UnitComponent.class)) {
            return entity.getComponent(UnitComponent.class);
        }
        else if (entity.hasComponent(StructureComponent.class)) {
            return entity.getComponent(StructureComponent.class);
        }
        else if (entity.hasComponent(ResourceComponent.class)) {
            return entity.getComponent(ResourceComponent.class);
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
