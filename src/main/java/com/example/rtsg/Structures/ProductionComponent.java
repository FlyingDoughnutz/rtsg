package com.example.rtsg.Structures;

import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.example.rtsg.EntityComponent;
import com.example.rtsg.Player;
import com.example.rtsg.PlayerManager;
import com.example.rtsg.Units.UnitDatabase;
import com.example.rtsg.Units.UnitStats;

import java.util.LinkedList;
import java.util.Queue;

import static com.almasb.fxgl.dsl.FXGL.spawn;

public class ProductionComponent extends Component {

    private final Player owner;
    private Queue<String> productionQueue = new LinkedList<>();

    private double timeRemaining = 0.0;
    private double currentTotalBuildTime = 0.0;

    public ProductionComponent(Player owner) {
        this.owner = owner;
    }

    @Override
    public void onUpdate(double tpf) {
        //If there's a unit in queue and timer not complete
        if (timeRemaining > 0) {
            timeRemaining -= tpf;
            //If we finished (including a little over by frame time) spawn the unit
            if (timeRemaining <= 0) {
                finishProduction();
            }
        }
        //If the current one has been produced, start production timer of the next unit
        else if(!productionQueue.isEmpty()){
            startNextUnit();
        }
    }

    //if conditions are right, spawn a unit beside the building (centre below by default)
    public void buildUnit(String unitName){
        UnitStats unitStats = UnitDatabase.getStats(unitName);
        int unitCost = unitStats.unitMineralCost();
        int unitSupply = unitStats.supplyCost();
        var masterStructureComponent = entity.getComponentOptional(StructureComponent.class);
        if (masterStructureComponent.isPresent()) {
            //check for resource
            if (owner.getMinerals() >= unitCost) {
                //check for supply
                if (owner.getMaxSupply() - owner.getCurrentSupply() >= unitSupply) {
                    //subtract minerals
                    owner.setMinerals(owner.getMinerals() - unitCost);

                    //adds to the queue
                    productionQueue.add(unitName);
                }
                else {
                    System.out.println("Not enough supply!");
                }
            }
            else {
                System.out.println("Not enough minerals!");
            }
        }

    }


    private void startNextUnit() {
        //TODO: implement
    }

    private void finishProduction() {
        //TODO: implement
    }
    

}
