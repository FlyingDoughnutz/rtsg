package com.example.rtsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerManager {

    private static PlayerManager pInstance = null;
    //initialised eagerly: the static helpers below can be reached before
    //getInstance() runs, which used to leave this list null
    private static final List<Player> playerList = new ArrayList<Player>();
    private static Player humanPlayer;

    private PlayerManager() {
    }

    public static PlayerManager getInstance() {
        if (pInstance == null){
            pInstance = new PlayerManager();
        }
        return pInstance;
    }

    public static List<Player> getPlayerList() {
        return playerList;
    }

    public static void addToPlayerList(Player player) {
        if (player == null) {
            System.out.println("Ignored attempt to add a null player");
            return;
        }
        if (!playerList.contains(player)) {
            playerList.add(player);
        }
    }

    public static Player findPlayerFromID(String playerID) {
        if (playerID == null) {
            return null;
        }
        for (Player player : playerList) {
            if (Objects.equals(player.getPlayerID(), playerID)){
                return player;
            }
        }
        return null;
    }

    public static void setHumanPlayer(Player player) {
        humanPlayer = player;
    }

    public static Player getHumanPlayer() {
        return humanPlayer;
    }

    //single source of truth for "does the human player own this entity".
    //call sites used to chain findPlayerFromID(...).getStance(...) with no
    //null check, so an unknown owner crashed the input handlers
    public static boolean isOwnedByHuman(EntityComponent component) {
        if (component == null || component.getOwner() == null || humanPlayer == null) {
            return false;
        }
        return humanPlayer.getStance(component.getOwner()) == Relation.SELF;
    }

    //same check for objects that only carry a raw Player, e.g. construction sites
    public static boolean isOwnedByHuman(Player owner) {
        if (owner == null || humanPlayer == null) {
            return false;
        }
        return humanPlayer.getStance(owner) == Relation.SELF;
    }

}
