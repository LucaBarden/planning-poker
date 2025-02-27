package de.lbarden.planningpoker.model;

import lombok.Data;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {
    private String id;
    private String name;
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private boolean revealed;
    private boolean reset;
    private long lastActivity;
    
    // Cache for player list to avoid creating new collection on each call
    private volatile Collection<Player> cachedPlayerList;
    private volatile int playerModificationCount = 0;

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

    // Synchronizing only when modifying players to ensure thread safety
    public synchronized void addPlayer(String playerId, Player player) {
        players.put(playerId, player);
        playerModificationCount++;
        // Invalidate cache
        cachedPlayerList = null;
    }
    
    public synchronized Player removePlayer(String playerId) {
        Player removed = players.remove(playerId);
        if (removed != null) {
            playerModificationCount++;
            // Invalidate cache
            cachedPlayerList = null;
        }
        return removed;
    }

    // Thread-safe optimized getter that uses cache
    public Collection<Player> getPlayerList() {
        Collection<Player> current = cachedPlayerList;
        if (current == null) {
            synchronized (this) {
                if (cachedPlayerList == null) {
                    // Create unmodifiable view to prevent external modification
                    cachedPlayerList = Collections.unmodifiableCollection(players.values());
                }
                current = cachedPlayerList;
            }
        }
        return current;
    }
}