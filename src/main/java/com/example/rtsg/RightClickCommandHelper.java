package com.example.rtsg;

import com.almasb.fxgl.entity.Entity;
import com.example.rtsg.Structures.BaseComponent;
import com.example.rtsg.Structures.StructureComponent;
import com.example.rtsg.Units.UnitComponent;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.Optional;

public class RightClickCommandHelper {

    static Optional<UnitComponent> getCommandableUnit(Entity unit) {
        if (unit == null || !unit.isActive()) {
            return Optional.empty();
        }
        return unit.getComponentOptional(UnitComponent.class)
                .filter(component -> component.hpProperty().get() > 0)
                .filter(PlayerManager::isOwnedByHuman);
    }

    public static void executeSmartCommand(List<Entity> selectedUnits, Entity target, Point2D mousePos) {
        if (target != null && (!target.isActive()
                || !FogOfWar.isVisibleTo(target, PlayerManager.getHumanPlayer()))) {
            target = null;
        }
        for  (Entity unit : selectedUnits) {
            UnitComponent unitComp = getCommandableUnit(unit).orElse(null);
            if (unitComp == null) continue;
            //clicked on ground
            if(target == null) {
                //use the position handed in with the click instead of re-polling
                //the mouse, so the command matches the click that triggered it
                unitComp.moveTo(mousePos);
                continue;
            }
            //clicked on unit, determine relationship
            EntityComponent targetComp = null;
            if (target.hasComponent(UnitComponent.class)) {
                targetComp = target.getComponent(UnitComponent.class);
            } else if (target.hasComponent(StructureComponent.class)) {
                targetComp = target.getComponent(StructureComponent.class);
            }
            //if target has logic and is unit or structure
            boolean isHostile = false;
            if (targetComp != null && targetComp.getOwner() != null) {
                isHostile = PlayerManager.getHumanPlayer()
                        .getStance(targetComp.getOwner()) == Relation.HOSTILE;
            }
            //if hostile, attack and move on
            if (isHostile) {
                unitComp.orderAttack(target);
                continue;
            }
            //a construction site: workers resume/join construction, others just walk to it
            if (target.getType() == EntityType.CONSTRUCTION) {
                if (unitComp.isWorker()) {
                    unitComp.orderBuild(target);
                }
                else {
                    unitComp.moveTo(target.getCenter());
                }
                continue;
            }
            //if resource, collect if worker, move if other unit
            if (target.getType() == EntityType.RESOURCE){
                if(unitComp.isWorker()) {
                    unitComp.mine(target);
                }
                else{
                    unitComp.moveTo(target.getCenter());
                }
                continue;
            }
            //if friendly structure, return if worker and base, move otherwise
            if (target.getType() == EntityType.STRUCTURE){
                //if cc
                if (target.hasComponent(BaseComponent.class)){
                    //return cargo if any
                    if (unitComp.isWorker() && unitComp.hasCargo()
                            && targetComp != null && PlayerManager.isOwnedByHuman(targetComp)) {
                        unitComp.orderReturnCargo(target);
                    }
                    //move otherwise
                    else {
                        unitComp.moveTo(target.getCenter());
                    }
                    continue;
                }
            }
            //default fallback logic is move
            Point2D targetCenter = target.getCenter().subtract(unit.getWidth()/2, unit.getHeight()/2);
            unitComp.moveTo(targetCenter);

        }
    }
}
