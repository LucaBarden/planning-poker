package com.example.planningpoker.model;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private String id;
    private String name;
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private boolean revealed;

    public Room() { }

    public Room(String id, String name) {
        this.id = id;
        this.name = name;
        this.revealed = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Map<String, Player> getPlayers() {
        return players;
    }
    public void setPlayers(Map<String, Player> players) {
        this.players = players;
    }
    public boolean isRevealed() {
        return revealed;
    }
    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    // Convenience method to get a list of players
    public Collection<Player> getPlayerList() {
        return players.values();
    }
}
