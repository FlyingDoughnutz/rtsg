package com.example.rtsg;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerManager {

    private static PlayerManager pInstance = null;
    private static List<Player> playerList;
    private static Player humanPlayer;

    private PlayerManager() {
        playerList = new ArrayList<Player>();
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
        playerList.add(player);
    }

    public static Player findPlayerFromID(String playerID) {
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

}
