package com.example.rtsg;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Player {

    private String playerID;
    private Color playerColor;
    private Map<Player, Relation> playerRelationMap;

    private IntegerProperty minerals = new SimpleIntegerProperty(0);
    private IntegerProperty gas = new SimpleIntegerProperty(0);
    private IntegerProperty currSupply = new SimpleIntegerProperty(0);
    private IntegerProperty maxSupply = new SimpleIntegerProperty(0);

    public Player(String playerID, Color playerColor) {
        this.playerID = playerID;
        this.playerColor = playerColor;
        playerRelationMap = new HashMap<Player, Relation>();
    }

    public IntegerProperty mineralsProperty() { return minerals; }
    public IntegerProperty gasProperty() { return gas; }
    public IntegerProperty currentSupplyProperty() { return currSupply; }
    public IntegerProperty maxSupplyProperty() { return maxSupply; }

    public void changeRelation(Player player, Relation relation) {
        playerRelationMap.put(player, relation);
    }

    public Relation getStance(Player otherPlayer){
        //return ally if it's the player itself
        if(otherPlayer.playerID.equals(this.playerID)){
            return Relation.SELF;
        }
        //otherwise check the relation
        else if (playerRelationMap.containsKey(otherPlayer)){
            return playerRelationMap.get(otherPlayer);
        }
        else{
            //default to neutral
            return Relation.NEUTRAL;
        }
    }

    public String getPlayerID() {
        return playerID;
    }

    public Color getPlayerColor() {
        return playerColor;
    }

    @Override
    public boolean equals(Object o){
        //if they're literally the same in memory return true
        if (this == o) return true;
        //if o isn't valid or isn't a valid object return false
        if (o == null || getClass() != o.getClass()) return false;
        //cast o to a Player
        Player player = (Player) o;
        //checks whether their id's are the same.
        return Objects.equals(playerID, player.playerID);
    }

    //forces playerID as hash
    @Override
    public int hashCode(){
        return Objects.hash(playerID);
    }

    public int getMinerals() {
        return minerals.get();
    }
    public void setMinerals(int minerals) {
        this.minerals.set(minerals);
    }

    public int getGas() {
        return gas.get();
    }
    public void setGas(int gas) {
        this.gas.set(gas);
    }

    public int getCurrentSupply() {
        return currSupply.get();
    }
    public void setCurrentSupply(int currentSupply) {
        this.currSupply.set(currentSupply);
    }

    public int getMaxSupply() {
        return maxSupply.get();
    }
    public void setMaxSupply(int maxSupply) {
        this.maxSupply.set(maxSupply);
    }

}
