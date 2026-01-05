package com.example.rtsg;

import com.almasb.fxgl.entity.Entity;
import com.example.rtsg.Structures.BaseComponent;
import com.example.rtsg.Structures.StructureComponent;
import com.example.rtsg.Units.UnitComponent;
import javafx.geometry.Point2D;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getInput;

public class RightClickCommandHelper {

    public static void executeSmartCommand(List<Entity> selectedUnits, Entity target, Point2D mousePos) {
        for  (Entity unit : selectedUnits) {
            //get unit component
            UnitComponent unitComp = unit.getComponentOptional(UnitComponent.class).orElse(null);
            //skip the entity if not unit
            if (unitComp == null) continue;
            if (PlayerManager.findPlayerFromID(unitComp.getOwner().getPlayerID()).getStance(PlayerManager.getHumanPlayer()) != Relation.SELF) {
                //skip if we can't control it
                continue;
            }
            //clicked on ground
            if(target == null) {
                System.out.println("Command: MOVE TO GROUND");
                unitComp.moveTo(getInput().getMousePositionWorld());
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
            if (targetComp != null) {
                Player targetOwner = PlayerManager.findPlayerFromID(targetComp.getOwner().getPlayerID());
                 isHostile = PlayerManager.getHumanPlayer().getStance(targetOwner) == Relation.HOSTILE;
            }
            //if hostile, attack and move on
            if (isHostile) {
                System.out.println("Command: ATTACK");
                unitComp.attack(target);
                continue;
            }
            //if resource, collect if worker, move if other unit
            if (target.getType() == EntityType.RESOURCE){
                if(unitComp.isWorker()) {
                    System.out.println("Command: MINE RESOURCE");
                    unitComp.mine(target);
                }
                else{
                    System.out.println("Command: MOVE TO RESOURCE");
                    unitComp.moveTo(target.getCenter());
                }
                continue;
            }
            //if friendly structure, return if worker and base, move otherwise
            if (target.getType() == EntityType.STRUCTURE){
                //if cc
                if (target.hasComponent(BaseComponent.class)){
                    //return cargo if any
                    if (unitComp.isWorker() && unitComp.hasCargo()) {
                        System.out.println("Command: RETURN RESOURCE");
                        unitComp.orderReturnCargo(target);
                    }
                    //move otherwise
                    else {
                        System.out.println("Command: MOVE TO FRIENDLY STRUCTURE");
                        unitComp.moveTo(target.getCenter());
                    }
                    continue;
                }
            }
            //default fallback logic is move
            System.out.println("Command: FOLLOW/MOVE TO UNIT");
            Point2D targetCenter = target.getCenter().subtract(unit.getWidth()/2, unit.getHeight()/2);
            unitComp.moveTo(targetCenter);

        }
    }
}
