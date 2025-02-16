package de.lbarden.planningpoker.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@Data
public class Room {
    private String id;
    private String name;
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private boolean revealed;
    private boolean reset;
    private long lastActivity;


    public Room() { }

    public Room(String id, String name) {
        this.id = id;
        this.name = name;
        this.revealed = false;
        updateLastActivity();
    }

    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    // Convenience method to get a list of players
    public Collection<Player> getPlayerList() {
        return players.values();
    }
}
