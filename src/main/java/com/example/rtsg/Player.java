package com.example.rtsg;

import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class Player {

    private String playerID;
    private Color playerColor;
    private Map<Player, Relation> playerRelationMap;
    public Player(String playerID, Color playerColor) {
        this.playerID = playerID;
        this.playerColor = playerColor;
        playerRelationMap = new HashMap<Player, Relation>();
    }

    public void changeRelation(Player player, Relation relation) {
        if (playerRelationMap.containsKey(player)) {
            playerRelationMap.remove(player);
        }
        playerRelationMap.put(player, relation);
    }

    public Relation getStance(Player otherPlayer){
        //return ally if it's the player itself
        if(otherPlayer.playerID == this.playerID){
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

}
