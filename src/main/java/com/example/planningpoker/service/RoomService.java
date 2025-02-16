package com.example.planningpoker.service;

import com.example.planningpoker.model.Player;
import com.example.planningpoker.model.Room;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom(String name) {
        String id = UUID.randomUUID().toString();
        Room room = new Room(id, name);
        rooms.put(id, room);
        return room;
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
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
        }
    }

    // Scheduled task: every minute check and remove rooms inactive for 60 minutes
    @Scheduled(fixedRate = 60000)
    public void removeStaleRoom() {
        long now = System.currentTimeMillis();
        long staleThreshold = 60 * 60 * 1000;
        Iterator<Map.Entry<String, Room>> iterator = rooms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Room> entry = iterator.next();
            Room room = entry.getValue();
            if (now - room.getLastActivity() > staleThreshold) {
                iterator.remove();
                System.out.println("Removed stale room: " + room.getId());
            }
        }
    }
}
