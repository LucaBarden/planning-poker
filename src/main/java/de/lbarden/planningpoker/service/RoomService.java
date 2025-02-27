package de.lbarden.planningpoker.service;

import de.lbarden.planningpoker.model.Player;
import de.lbarden.planningpoker.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private static final int MAX_ROOMS = 1000; // Prevent unbounded growth
    private static final long STALE_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(60);

    // Initial capacity and load factor to avoid frequent resizing
    private final Map<String, Room> rooms = new ConcurrentHashMap<>(16, 0.75f, 2);
    
    // Cache for frequently accessed rooms
    private Room lastAccessedRoom;
    private String lastAccessedRoomId;

    public Room createRoom(String name) {
        // Check if we've hit the room limit
        if (rooms.size() >= MAX_ROOMS) {
            logger.warn("Maximum room limit reached ({}). Cleaning up oldest rooms.", MAX_ROOMS);
            removeOldestRooms(MAX_ROOMS / 10); // Remove 10% of rooms
        }
        
        String id = UUID.randomUUID().toString();
        Room room = new Room(id, name);
        rooms.put(id, room);
        
        // Update cache
        lastAccessedRoom = room;
        lastAccessedRoomId = id;
        
        return room;
    }

    public Room getRoom(String roomId) {
        // Check cache first
        if (roomId != null && roomId.equals(lastAccessedRoomId) && lastAccessedRoom != null) {
            return lastAccessedRoom;
        }
        
        // Cache miss, get from map
        Room room = rooms.get(roomId);
        
        // Update cache
        if (room != null) {
            lastAccessedRoom = room;
            lastAccessedRoomId = roomId;
        }
        
        return room;
    }

    public void addPlayer(String roomId, Player player) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.getPlayers().put(player.getId(), player);
            room.updateLastActivity();
        }
    }

    public void updatePlayerCard(String roomId, String playerId, String card) {
        Room room = getRoom(roomId);
        if (room != null) {
            Player player = room.getPlayers().get(playerId);
            if (player != null) {
                player.setCard(card);
                room.updateLastActivity();
            }
        }
    }

    public void revealCards(String roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.setRevealed(true);
            room.updateLastActivity();
        }
    }

    public void resetRoom(String roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.setRevealed(false);
            room.setReset(true);
            room.getPlayers().values().forEach(player -> player.setCard(""));
            room.updateLastActivity();
        }
    }

    public void startRoom(String roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.setReset(false);
            room.updateLastActivity();
        }
    }

    public void removePlayer(String roomId, String playerId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.getPlayers().remove(playerId);
            room.updateLastActivity();
            
            // If room is empty, remove it
            if (room.getPlayers().isEmpty()) {
                rooms.remove(roomId);
                // Clear cache if it was the cached room
                if (roomId.equals(lastAccessedRoomId)) {
                    lastAccessedRoom = null;
                    lastAccessedRoomId = null;
                }
                logger.info("Removed empty room: {}", roomId);
            }
        }
    }

    // Remove oldest rooms when we hit capacity
    private void removeOldestRooms(int count) {
        rooms.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((r1, r2) -> 
                Long.compare(r1.getLastActivity(), r2.getLastActivity())))
            .limit(count)
            .forEach(entry -> {
                rooms.remove(entry.getKey());
                logger.info("Removed old room due to capacity: {}", entry.getKey());
            });
    }

    // Scheduled task: every minute check and remove rooms inactive for 60 minutes
    @Scheduled(fixedRate = 60000)
    public void removeStaleRooms() {
        long now = System.currentTimeMillis();
        int removedCount = 0;
        
        Iterator<Map.Entry<String, Room>> iterator = rooms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Room> entry = iterator.next();
            Room room = entry.getValue();
            if (now - room.getLastActivity() > STALE_THRESHOLD_MS) {
                iterator.remove();
                // Clear cache if it was the cached room
                if (entry.getKey().equals(lastAccessedRoomId)) {
                    lastAccessedRoom = null;
                    lastAccessedRoomId = null;
                }
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Removed {} stale rooms", removedCount);
        }
    }
}