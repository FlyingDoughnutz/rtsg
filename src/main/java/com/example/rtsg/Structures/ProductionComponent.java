package com.example.rtsg.Structures;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.EntityType;
import com.example.rtsg.Player;
import com.example.rtsg.Units.UnitComponent;
import com.example.rtsg.Units.UnitDatabase;
import com.example.rtsg.Units.UnitStats;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class ProductionComponent extends Component {
    private static final System.Logger LOG = System.getLogger(ProductionComponent.class.getName());
    // Covers every current unit's bounding box, including its local hitbox offset.
    private static final double SPAWN_SIZE = 40;
    private static final double SPAWN_GAP = 8;

    private record Order(String unitName, UnitStats stats) {}

    private final Player owner;
    private final Deque<Order> productionQueue = new ArrayDeque<>();
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyIntegerWrapper queueSize = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Idle");
    private double timeRemaining;
    private double spawnRetryRemaining;
    private double feedbackRemaining;
    private String feedback = "";
    private String activity = "Idle";
    /** Where freshly produced units should walk; shown as a red pin while the building is selected. */
    private Point2D rallyPoint;

    public ProductionComponent(Player owner) {
        this.owner = owner;
    }

    public Point2D getRallyPoint() {
        return rallyPoint;
    }

    public void setRallyPoint(Point2D rallyPoint) {
        this.rallyPoint = rallyPoint;
    }

    public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }
    public ReadOnlyIntegerProperty queueSizeProperty() { return queueSize.getReadOnlyProperty(); }
    public ReadOnlyStringProperty statusProperty() { return status.getReadOnlyProperty(); }

    public boolean buildUnit(String unitName) {
        if (!canProduce()) return false;
        UnitStats stats = UnitDatabase.getStats(unitName);
        if (stats == null || stats.buildTime() <= 0) {
            showFeedback("Unknown unit");
            return false;
        }
        if (owner.getMinerals() < stats.unitMineralCost()) {
            showFeedback("Not enough minerals");
            return false;
        }
        if (!owner.tryReserveSupply(stats.supplyCost())) {
            showFeedback("Not enough supply");
            return false;
        }

        owner.setMinerals(owner.getMinerals() - stats.unitMineralCost());
        productionQueue.addLast(new Order(unitName.toLowerCase(Locale.ROOT), stats));
        queueSize.set(productionQueue.size());
        clearFeedback();
        if (productionQueue.size() == 1) startNextUnit();
        return true;
    }

    @Override
    public void onUpdate(double tpf) {
        if (!canProduce() || tpf <= 0 || !Double.isFinite(tpf)) return;
        if (feedbackRemaining > 0) {
            feedbackRemaining -= tpf;
            if (feedbackRemaining <= 0) clearFeedback();
        }
        spawnRetryRemaining = Math.max(0, spawnRetryRemaining - tpf);
        double elapsed = tpf;
        // Carry excess frame time into the next order so low FPS does not slow production.
        while (!productionQueue.isEmpty()) {
            Order current = productionQueue.peekFirst();
            double consumed = Math.min(elapsed, timeRemaining);
            timeRemaining -= consumed;
            elapsed -= consumed;
            progress.set(1 - timeRemaining / current.stats().buildTime());
            if (timeRemaining > 0 || !finishProduction()) break;
            startNextUnit();
            if (elapsed <= 0) break;
        }
    }

    private boolean canProduce() {
        if (owner == null || entity == null || !entity.isActive()) return false;
        return entity.getComponentOptional(StructureComponent.class)
                .filter(structure -> structure.getOwner() == owner && structure.hpProperty().get() > 0)
                .isPresent();
    }

    private void startNextUnit() {
        Order current = productionQueue.peekFirst();
        timeRemaining = current == null ? 0 : current.stats().buildTime();
        spawnRetryRemaining = 0;
        progress.set(0);
        setActivity(current == null ? "Idle" : "Training " + current.stats().name());
    }

    private boolean finishProduction() {
        Order current = productionQueue.peekFirst();
        // Existing reservations survive a supply-provider loss, but cannot spawn over the cap.
        if ((long) owner.getCurrentSupply() + current.stats().supplyCost() > owner.getMaxSupply()) {
            setActivity("Waiting for supply: " + current.stats().name());
            return false;
        }
        if (spawnRetryRemaining > 0) return false;
        Point2D spawnPosition = findSpawnPosition();
        if (spawnPosition == null) {
            setActivity("Exit blocked: " + current.stats().name());
            spawnRetryRemaining = 0.25;
            return false;
        }
        try {
            Entity spawned = entity.getWorld().spawn(current.unitName(),
                    new SpawnData(spawnPosition).put("owner", owner));
            //newly produced units walk to the building's rally point, if one is set
            if (rallyPoint != null) {
                spawned.getComponentOptional(UnitComponent.class).ifPresent(unit -> unit.moveTo(rallyPoint));
            }
        } catch (RuntimeException error) {
            // A missing/broken factory must not charge the player forever or retry every frame.
            productionQueue.removeFirst();
            owner.releaseReservedSupply(current.stats().supplyCost());
            owner.setMinerals(owner.getMinerals() + current.stats().unitMineralCost());
            queueSize.set(productionQueue.size());
            showFeedback("Production failed; minerals refunded");
            LOG.log(System.Logger.Level.ERROR, "Unable to produce " + current.unitName(), error);
            return true;
        }
        // UnitComponent.onAdded accounts for the live unit; release only its reservation here.
        productionQueue.removeFirst();
        owner.releaseReservedSupply(current.stats().supplyCost());
        queueSize.set(productionQueue.size());
        return true;
    }

    private Point2D findSpawnPosition() {
        double step = SPAWN_SIZE + SPAWN_GAP;
        double centerX = entity.getCenter().getX() - SPAWN_SIZE / 2;
        double centerY = entity.getCenter().getY() - SPAWN_SIZE / 2;
        var properties = entity.getWorld().getProperties();
        double mapWidth = properties.<Number>getValueOptional("mapWidth")
                .map(Number::doubleValue).orElse(Double.POSITIVE_INFINITY);
        double mapHeight = properties.<Number>getValueOptional("mapHeight")
                .map(Number::doubleValue).orElse(Double.POSITIVE_INFINITY);
        // Try below first, then the other sides. Nearby units never spawn on top of each other.
        for (int offset = 0; offset <= 3; offset++) {
            for (int sign : offset == 0 ? new int[]{1} : new int[]{1, -1}) {
                double shift = offset * sign * step;
                Point2D[] candidates = {
                        new Point2D(centerX + shift, entity.getBottomY() + SPAWN_GAP),
                        new Point2D(centerX + shift, entity.getY() - SPAWN_SIZE - SPAWN_GAP),
                        new Point2D(entity.getRightX() + SPAWN_GAP, centerY + shift),
                        new Point2D(entity.getX() - SPAWN_SIZE - SPAWN_GAP, centerY + shift)
                };
                for (Point2D candidate : candidates) {
                    if (candidate.getX() < 0 || candidate.getY() < 0
                            || candidate.getX() + SPAWN_SIZE > mapWidth
                            || candidate.getY() + SPAWN_SIZE > mapHeight) continue;
                    Rectangle2D footprint = new Rectangle2D(candidate.getX(), candidate.getY(),
                            SPAWN_SIZE, SPAWN_SIZE);
                    boolean occupied = entity.getWorld().getEntitiesInRange(footprint).stream()
                            .anyMatch(e -> e.isActive() && (e.isType(EntityType.UNIT)
                                    || e.isType(EntityType.STRUCTURE) || e.isType(EntityType.RESOURCE)
                                    || e.isType(EntityType.CONSTRUCTION)));
                    if (!occupied) return candidate;
                }
            }
        }
        return null;
    }

    public boolean cancelCurrentUnit() {
        return cancelUnit(true);
    }

    public boolean cancelLastUnit() {
        return cancelUnit(false);
    }

    private boolean cancelUnit(boolean current) {
        if (!canProduce() || productionQueue.isEmpty()) return false;
        boolean restart = current || productionQueue.size() == 1;
        Order cancelled = current ? productionQueue.removeFirst() : productionQueue.removeLast();
        owner.releaseReservedSupply(cancelled.stats().supplyCost());
        owner.setMinerals(owner.getMinerals() + cancelled.stats().unitMineralCost());
        queueSize.set(productionQueue.size());
        clearFeedback();
        if (restart) startNextUnit();
        return true;
    }

    @Override
    public void onRemoved() {
        // Destruction loses paid minerals, but must free reservations in every pending order.
        for (Order order : productionQueue) owner.releaseReservedSupply(order.stats().supplyCost());
        productionQueue.clear();
        queueSize.set(0);
        clearFeedback();
        startNextUnit();
    }

    private void setActivity(String value) {
        activity = value;
        refreshStatus();
    }

    private void showFeedback(String message) {
        feedback = message;
        feedbackRemaining = 2;
        refreshStatus();
    }

    private void clearFeedback() {
        feedback = "";
        feedbackRemaining = 0;
        refreshStatus();
    }

    private void refreshStatus() {
        status.set(feedback.isEmpty() ? activity : activity + " — " + feedback);
    }
}
