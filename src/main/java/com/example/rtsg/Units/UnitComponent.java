package com.example.rtsg.Units;

import com.almasb.fxgl.entity.Entity;
import com.example.rtsg.*;
import com.example.rtsg.Buildings.ConstructionComponent;
import com.example.rtsg.Resources.ResourceComponent;
import com.example.rtsg.Structures.BaseComponent;
import com.example.rtsg.Structures.StructureComponent;
import javafx.beans.property.*;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import java.util.Comparator;

import static java.lang.System.Logger.Level.DEBUG;

public class UnitComponent extends EntityComponent {

    private static final System.Logger LOGGER = System.getLogger(UnitComponent.class.getName());

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
    private Point2D finalVelocity = Point2D.ZERO;
    private boolean flagIsStuck = false;

    private final int COLLECTION_RATE = 1;
    private final int COLLECTION_CAPACITY = 5;
    private double miningTimer = 0.0;
    private final int MINERAL_SCAN_RANGE = 200;
    //separate from MINERAL_SCAN_RANGE so "how far a worker looks for ore" and
    //"how far a worker looks for a drop-off" can be tuned independently
    private final int BASE_SCAN_RANGE = 200;
    //extra reach past the unit radius when testing "am I next to that rock/base"
    private static final double GATHER_REACH_SLACK = 8.0;
    private double scanMiningTimer = 0.0;
    private double scanCCTimer = 0.0;
    private Entity miningTarget;
    private Entity returnTarget;
    private Entity buildTarget;
    private int resourceCarried = 0;
    //progress tracking for gathering/building: distance to the goal only ever has
    //to improve, so a unit that jitters in place without getting closer still
    //counts as stuck
    private double approachBestDistance = Double.MAX_VALUE;
    private double approachNoProgressTime = 0.0;

    public UnitComponent(UnitStats stats, Player owner) {
        super(owner);
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
        this.armor = new SimpleIntegerProperty(stats.armor());
        this.isWorker = new SimpleBooleanProperty(stats.isWorker());
    }

    @Override
    public void onAdded() {
        //this component keeps its own death listener, so it must NOT call
        //super.onAdded() or the unit would be killed twice
        hpProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() <= 0) onDeath();
        });
        //update supply when created
        Player owner = getOwner();
        if (owner != null) {
            owner.setCurrentSupply(owner.getCurrentSupply() + stats.supplyCost());
        }
    }

    @Override
    public void onRemoved() {
        //update supply when destroyed
        Player owner = getOwner();
        if (owner != null) {
            //clamp to prevent going negative
            int newSupply = Math.max(0, owner.getCurrentSupply() - stats.supplyCost());
            owner.setCurrentSupply(newSupply);
        }
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

        finalVelocity = Point2D.ZERO;

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
                break;

            case MOVING:
                updateMoveLogic();
                if (moveTarget != null) calculateMovementPhysics();
                else state = UnitState.IDLE;
                break;

            case ATTACK:
                if (attackTarget == null || !attackTarget.isActive()
                        || !FogOfWar.isVisibleTo(attackTarget, getOwner())){
                    attackTarget = null;
                    moveTarget = null;
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
                    //an attack-move that reaches its destination while shooting
                    //has arrived: stop chasing and drop the stale destination
                    else if (attackMoveDestination != null
                            && entity.getCenter().distance(attackMoveDestination) < 10) {
                        attackMoveDestination = null;
                        moveTarget = null;
                        attackTarget = null;
                        state = UnitState.IDLE;
                        flagIsStuck = false;
                        settleTimer = 0.0;
                    }
                    //idle while attacking: still track position so that giving up a
                    //target that sits behind an obstacle eventually works
                    else {
                        updateStuckDetection();
                        //gave up on the target while stationary
                        if (flagIsStuck && settleTimer > 0.8) {
                            attackTarget = null;
                            moveTarget = null;
                            flagIsStuck = false;
                            settleTimer = 0.0;
                            state = UnitState.IDLE;
                        }
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
                //clear the stored destination once it has actually been reached,
                //otherwise a unit that fights on the way would later resume a
                //path it already finished
                if (attackMoveDestination != null
                        && entity.getCenter().distance(attackMoveDestination) < 10) {
                    attackMoveDestination = null;
                    moveTarget = null;
                    state = UnitState.IDLE;
                    flagIsStuck = false;
                    settleTimer = 0.0;
                }
                else if (moveTarget != null) calculateMovementPhysics();
                else state = UnitState.IDLE;
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

            case GATHER: {
                //A full load is delivered first. The capacity check used to run only
                //after a successful mine, so a worker whose load was already full
                //could sit in GATHER forever whenever it could not reach a patch.
                if (resourceCarried >= COLLECTION_CAPACITY) {
                    if (!returnToNearestBase()) {
                        state = UnitState.IDLE;
                    }
                    break;
                }
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
                        resetPathProgress();
                    }
                    //no minerals found
                    else {
                        state = UnitState.IDLE;
                    }
                    break;
                }
                //move towards mineral
                if (moveTarget == null) moveTarget = miningTarget.getCenter();
                //arrive test against the patch's bounding box instead of its centre:
                //the centre test only cleared the collision push-out by a few pixels
                //depending on approach direction, so wedged workers never "arrived"
                double distToMine = distanceToBounds(miningTarget);
                if (distToMine <= gatherReach()) {
                    //stop moving before mining
                    moveTarget = null;
                    finalVelocity = Point2D.ZERO;
                    flagIsStuck = false;
                    settleTimer = 0.0;
                    if (miningTimer <= 0.0) {
                        //mine
                        performMining();
                        miningTimer = 1.0;
                        //return cargo
                        if (resourceCarried >= COLLECTION_CAPACITY) {
                            if (!returnToNearestBase()) {
                                state = UnitState.IDLE;
                            }
                        }
                    }
                }
                //still too far from the resource
                else {
                    if (updateApproachProgress(distToMine, timePerFrame)) {
                        //wedged before reaching the patch: give up on it, try another
                        Entity previous = miningTarget;
                        miningTarget = null;
                        moveTarget = null;
                        resetPathProgress();
                        miningTarget = scanAroundMine(previous);
                        if (miningTarget == null) {
                            state = UnitState.IDLE;
                        }
                    }
                    else {
                        calculateMovementPhysics();
                    }
                }
                break;
            }

            case RETURN_CARGO: {
                //check for return cc, and re-validate one we already hold
                if (returnTarget == null || !returnTarget.isActive()) {
                    returnTarget = scanAroundCC();
                    //can't find cc -> idle
                    if (returnTarget == null) {
                        state = UnitState.IDLE;
                        break;
                    }
                }
                //move towards cc
                if (moveTarget == null) moveTarget = returnTarget.getCenter();
                //drop off once adjacent to the base, using the same box test as mining
                double distToCC = distanceToBounds(returnTarget);
                if (distToCC <= gatherReach()) {
                    returnResources();
                    resetPathProgress();
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
                    if (updateApproachProgress(distToCC, timePerFrame)) {
                        //blocked on the way to the base: stop instead of freezing
                        //forever; the player can issue a new order
                        returnTarget = null;
                        moveTarget = null;
                        resetPathProgress();
                        state = UnitState.IDLE;
                    }
                    else {
                        calculateMovementPhysics();
                    }
                }
                break;
            }

            case MOVE_TO_BUILD: {
                ConstructionComponent site = constructionSite(buildTarget);
                if (site == null) {
                    //site finished, was cancelled or was destroyed
                    buildTarget = null;
                    moveTarget = null;
                    resetPathProgress();
                    state = UnitState.IDLE;
                    break;
                }
                if (moveTarget == null) moveTarget = buildTarget.getCenter();
                double distToSite = distanceToBounds(buildTarget);
                if (distToSite <= gatherReach()) {
                    //close enough to start constructing
                    moveTarget = null;
                    finalVelocity = Point2D.ZERO;
                    resetPathProgress();
                    state = UnitState.BUILDING;
                }
                else if (updateApproachProgress(distToSite, timePerFrame)) {
                    //cannot reach the site: drop the order instead of freezing
                    buildTarget = null;
                    moveTarget = null;
                    resetPathProgress();
                    state = UnitState.IDLE;
                }
                else {
                    calculateMovementPhysics();
                }
                break;
            }

            case BUILDING: {
                ConstructionComponent site = constructionSite(buildTarget);
                if (site == null) {
                    //finished; the site replaces itself with the real building
                    buildTarget = null;
                    moveTarget = null;
                    resetPathProgress();
                    state = UnitState.IDLE;
                    break;
                }
                //if pushed away by other workers, walk back to the site
                if (distanceToBounds(buildTarget) > gatherReach()) {
                    moveTarget = buildTarget.getCenter();
                    resetPathProgress();
                    state = UnitState.MOVE_TO_BUILD;
                    break;
                }
                moveTarget = null;
                finalVelocity = Point2D.ZERO;
                //several workers stack their build time; completion is handled by the site
                site.contribute(timePerFrame);
                break;
            }

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
        clearCurrentOrder();
        this.returnTarget = baseTarget;
        setState(UnitState.RETURN_CARGO);
    }

    public void orderPatrol(Point2D patrolTarget){
        clearCurrentOrder();
        this.patrolPointA = entity.getCenter();
        this.patrolPointB = patrolTarget;
        this.currentPatrolTarget = patrolPointB;
        this.moveTarget = currentPatrolTarget;
        this.flagPatrol = true;
        setState(UnitState.PATROL);
    }

    public void orderHold(){
        clearCurrentOrder();
        this.flagHold = true;
        setState(UnitState.HOLD);
    }

    public void moveAttack(Point2D target) {
        clearCurrentOrder();
        this.moveTarget = target;
        this.attackMoveDestination = target;
        setState(UnitState.ATTACK_MOVE);
    }

    public void orderAttack(Entity target) {
        if (!FogOfWar.isVisibleTo(target, getOwner())) return;
        clearCurrentOrder();
        attack(target);
    }

    public void stop() {
        clearCurrentOrder();
        setState(UnitState.IDLE);
    }

    //Explicit commands replace the previous order; combat/mining cooldowns and cargo survive.
    private void clearCurrentOrder() {
        moveTarget = null;
        attackTarget = null;
        attackMoveDestination = null;
        miningTarget = null;
        returnTarget = null;
        buildTarget = null;
        flagPatrol = false;
        flagHold = false;
        patrolPointA = null;
        patrolPointB = null;
        currentPatrolTarget = null;
        flagIsStuck = false;
        settleTimer = 0.0;
        lastPosition = entity.getCenter();
        finalVelocity = Point2D.ZERO;
        //a new order must not inherit progress bookkeeping from the previous target
        approachBestDistance = Double.MAX_VALUE;
        approachNoProgressTime = 0.0;
    }

    //helper that scans around the current unit and finds a valid hostile target
    public Entity scanAroundAttack() {
        //find an appropriate scan radius for this unit
        //if ranged, slightly larger, if melee, set to 250
        double scanRadius = Math.max(stats.attackRange() * 1.5, 250);

        Point2D center = entity.getCenter();
        var foundEntities = entity.getWorld().getEntitiesInRange(
                new Rectangle2D(center.getX() - scanRadius, center.getY() - scanRadius,
                        scanRadius * 2, scanRadius * 2));
        return foundEntities.stream()
                .filter(e -> e != entity)//has to be an entity
                .filter(e -> e.isType(EntityType.UNIT) || e.isType(EntityType.STRUCTURE))//has to be a unit or a structure
                .filter(e -> isHostile(e))//filter for hostile
                .filter(e -> FogOfWar.isVisibleTo(e, getOwner()))
                .min(Comparator.comparingDouble(e -> e.distance(entity)))
                .orElse(null);//return null if no valid target
    }

    //helper that scans around the current unit and finds a valid mineral patch
    public Entity scanAroundMine() {
        return scanAroundMine(null);
    }

    //nearest mineral patch, optionally ignoring one that just proved unreachable
    private Entity scanAroundMine(Entity exclude) {

        Point2D center = entity.getCenter();
        var foundEntities = entity.getWorld().getEntitiesInRange(
                new Rectangle2D(center.getX() - MINERAL_SCAN_RANGE, center.getY() - MINERAL_SCAN_RANGE,
                        MINERAL_SCAN_RANGE * 2, MINERAL_SCAN_RANGE * 2));
        return foundEntities.stream()
                .filter(e -> e != entity)//has to be an entity
                .filter(e -> e != exclude)//skip the patch we just gave up on
                .filter(e -> e.isType(EntityType.RESOURCE))//has to be a resource
                .filter(e -> e.hasComponent(ResourceComponent.class))//filter resource with logic
                .filter(e -> e.getComponent(ResourceComponent.class).getResourceType() == 0)//filters for minerals
                .filter(e -> e.getComponent(ResourceComponent.class).hpProperty().get() > 0)
                .filter(e -> FogOfWar.isVisibleTo(e, getOwner()))
                .min(Comparator.comparingDouble(e -> e.distance(entity)))
                .orElse(null);//return null if no valid target
    }

    //finds a nearby cc owned by this unit's player
    public Entity scanAroundCC() {
        Point2D center = entity.getCenter();
        var foundEntities = entity.getWorld().getEntitiesInRange(
                new Rectangle2D(center.getX() - BASE_SCAN_RANGE, center.getY() - BASE_SCAN_RANGE,
                        BASE_SCAN_RANGE * 2, BASE_SCAN_RANGE * 2));
        return foundEntities.stream()
                .filter(e -> e != entity)//has to be an entity
                .filter(Entity::isActive)//ignore bases that were already destroyed
                .filter(e -> e.isType(EntityType.STRUCTURE))//has to be a structure
                .filter(e -> e.hasComponent(StructureComponent.class))//filter structure with logic
                .filter(e -> e.hasComponent(BaseComponent.class))//filters for bases
                //only drop off at our own (or an ally's) base, never at an enemy's
                .filter(e -> {
                    StructureComponent base = e.getComponent(StructureComponent.class);
                    if (base.getOwner() == null) return false;
                    Relation stance = this.getOwner() != null
                            ? this.getOwner().getStance(base.getOwner())
                            : Relation.NEUTRAL;
                    return stance == Relation.SELF || stance == Relation.ALLY;
                })
                .min(Comparator.comparingDouble(e -> e.distance(entity)))
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
        LOGGER.log(DEBUG, "Not a real resource mined!");
    }

    //how close this unit's centre must get to a target's bounds to act on it
    private double gatherReach() {
        return entity.getWidth() / 2.0 + GATHER_REACH_SLACK;
    }

    //distance from this unit's centre to the nearest point of the target's bounds
    //(0 when the centre is inside). Direction-independent, unlike a centre-to-centre
    //test whose clearance depends on which side the unit approaches from.
    private double distanceToBounds(Entity target) {
        Point2D center = entity.getCenter();
        double minX = target.getX();
        double minY = target.getY();
        double maxX = minX + target.getWidth();
        double maxY = minY + target.getHeight();
        double closestX = Math.max(minX, Math.min(center.getX(), maxX));
        double closestY = Math.max(minY, Math.min(center.getY(), maxY));
        return center.distance(closestX, closestY);
    }

    //forget the movement-progress bookkeeping so a fresh path starts clean
    private void resetPathProgress() {
        flagIsStuck = false;
        settleTimer = 0.0;
        lastPosition = entity.getCenter();
        approachBestDistance = Double.MAX_VALUE;
        approachNoProgressTime = 0.0;
    }

    //True once the unit has spent too long without getting measurably closer to
    //its goal (ore patch, drop-off base or construction site). Raw movement
    //cannot detect this: a unit can jitter a few pixels per frame against a wall
    //or another unit forever.
    private boolean updateApproachProgress(double distanceToTarget, double timePerFrame) {
        if (distanceToTarget < approachBestDistance - 1.0) {
            approachBestDistance = distanceToTarget;
            approachNoProgressTime = 0.0;
            return false;
        }
        approachNoProgressTime += timePerFrame;
        return approachNoProgressTime > 0.8;
    }

    //send the worker back to the nearest own base; false when there is none
    private boolean returnToNearestBase() {
        if (returnTarget == null || !returnTarget.isActive()) {
            returnTarget = scanAroundCC();
        }
        if (returnTarget == null) {
            LOGGER.log(DEBUG, "No command centre found!");
            return false;
        }
        moveTarget = null;
        resetPathProgress();
        setState(UnitState.RETURN_CARGO);
        return true;
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
            //check existence FIRST: getEntityComponent returns null when the
            //target lost its component (e.g. already removed from the world),
            //and the old order dereferenced it before the null test
            if (targetStats == null || !attackTarget.isActive() || targetStats.hpProperty().get() <= 0) {
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
            if (distance > 0) {
                moveDir = moveVector.multiply(currentSpeed / distance);
            }
        }
        //add this separation to the existing velocity (regular movement)
        finalVelocity = moveDir.add(separation);
        //if it's faster than intended, clamp it to the unit's speed
        double speed = finalVelocity.magnitude();
        if (speed > stats.moveSpeed()) finalVelocity = finalVelocity.multiply(stats.moveSpeed() / speed);
    }

    //check if hostile to this
    private boolean isHostile(Entity e) {

        EntityComponent targetComponent = getEntityComponent(e);
        //check for nulls
        if (getOwner() == null || targetComponent == null || targetComponent.getOwner() == null) return false;

        //find player using ID instead of getOwner
        Player actualOwner = PlayerManager.findPlayerFromID(this.getOwner().getPlayerID());

        Player actualTarget = PlayerManager.findPlayerFromID(targetComponent.getOwner().getPlayerID());

        if (actualOwner == null || actualTarget == null) return false;

        return actualOwner.getStance(actualTarget) == Relation.HOSTILE;
    }

    public void moveTo(Point2D moveTarget){
        clearCurrentOrder();
        this.moveTarget = moveTarget;
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
        double separationX = 0.0;
        double separationY = 0.0;
        Point2D myCenter = entity.getCenter();

        double unitRadius = entity.getWidth() / 2.0;
        double searchDist = unitRadius * 4.0 + 50.0;
        double moveSpeed = stats.moveSpeed();

        Rectangle2D detectionArea = new Rectangle2D(
                entity.getX() - searchDist,
                entity.getY() - searchDist,
                searchDist * 2,
                searchDist * 2
        );

        var nearbyUnits = entity.getWorld().getEntitiesInRange(detectionArea);

        for (Entity other: nearbyUnits){
            if (other == entity) continue;

            //Rectangle Structure collision (finished buildings and construction sites)
            if (other.isType(EntityType.STRUCTURE) || other.isType(EntityType.RESOURCE)
                    || other.isType(EntityType.CONSTRUCTION)){
                //get the exact bounds of the structure/resource
                double bMinX = other.getX();
                double bMinY = other.getY();
                double bMaxX = bMinX + other.getWidth();
                double bMaxY = bMinY + other.getHeight();
                //find the closest point on the building to the center of this entity
                double closestX = Math.max(bMinX, Math.min(myCenter.getX(), bMaxX));
                double closestY = Math.max(bMinY, Math.min(myCenter.getY(), bMaxY));
                double deltaX = myCenter.getX() - closestX;
                double deltaY = myCenter.getY() - closestY;
                double distanceSquared = deltaX * deltaX + deltaY * deltaY;
                double requiredSpace = unitRadius + 2.0;
                //Reject non-overlaps before calculating a distance or allocating vectors.
                if (distanceSquared < requiredSpace * requiredSpace){
                    double distToWall = Math.sqrt(distanceSquared);
                    double overlap = requiredSpace - distToWall;
                    double pushForceScale = Math.min(overlap / 10.0, 1.0);
                    double pushSpeed = moveSpeed * 5.0 * pushForceScale;
                    if (distToWall > 0) {
                        separationX += deltaX / distToWall * pushSpeed;
                        separationY += deltaY / distToWall * pushSpeed;
                    } else {
                        separationX += pushSpeed;
                    }
                }

            }

            //Circular Unit collision
            else if (other.isType(EntityType.UNIT)){
                //gets the size of the other unit to collide with
                double otherRadius = other.getWidth() / 2.0;
                //space between the 2 units should be their conbined radius
                double requiredSpace = unitRadius + otherRadius;
                //distance from this center to the other's center
                Point2D otherCenter = other.getCenter();
                double deltaX = myCenter.getX() - otherCenter.getX();
                double deltaY = myCenter.getY() - otherCenter.getY();
                double distanceSquared = deltaX * deltaX + deltaY * deltaY;
                //if they're not overlapping but too close together
                if (distanceSquared < requiredSpace * requiredSpace && distanceSquared > 0){
                    double dist = Math.sqrt(distanceSquared);
                    //strength is stronger the deeper the overlap is
                    double strength = (requiredSpace - dist) / requiredSpace;
                    separationX += deltaX / dist * moveSpeed * strength;
                    separationY += deltaY / dist * moveSpeed * strength;
                }
            }

        }

        return separationX == 0 && separationY == 0
                ? Point2D.ZERO
                : new Point2D(separationX, separationY);
    }

    //performs the attack
    private void performAttack() {
        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "{0} attacked {1}", stats.name(), attackTarget.getType());
        }

        EntityComponent targetStats = getEntityComponent(attackTarget);

        if  (targetStats != null) {
            int currentHp = targetStats.hpProperty().get();
            int damage = (int) stats.attackDamage();
            int armor = targetStats.armorProperty() != null ? targetStats.armorProperty().get() : 0;
            //armour must never turn an attack into healing: keep a floor of 1
            int effectiveDamage = Math.max(1, damage - armor);
            targetStats.hpProperty().set(Math.max(0, currentHp - effectiveDamage));
        }

    }

    //Auto-acquired targets temporarily interrupt patrol, hold, or attack-move.
    //Player attack commands use orderAttack so they replace those orders.
    public void attack(Entity target){
        this.attackTarget = target;
        this.moveTarget = null;
        this.flagIsStuck = false;
        this.settleTimer = 0.0;
        setState(UnitState.ATTACK);
    }

    public void mine(Entity target){
        clearCurrentOrder();
        this.miningTarget = target;
        setState(UnitState.GATHER);
    }

    //worker walks to a construction site and adds build progress while there
    public void orderBuild(Entity site) {
        clearCurrentOrder();
        this.buildTarget = site;
        setState(UnitState.MOVE_TO_BUILD);
    }

    public Entity getBuildTarget() {
        return buildTarget;
    }

    //the construction site this worker is assigned to, if it still needs building
    private ConstructionComponent constructionSite(Entity target) {
        if (target == null || !target.isActive()) {
            return null;
        }
        ConstructionComponent site = target.getComponentOptional(ConstructionComponent.class).orElse(null);
        return site != null && !site.isComplete() ? site : null;
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
        if (this.state != state && LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "{0} changed state to: {1}", name.get(), state);
        }
        this.state = state;
    }
}
