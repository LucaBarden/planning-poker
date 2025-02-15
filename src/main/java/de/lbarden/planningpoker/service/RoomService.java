package de.lbarden.planningpoker.service;

import de.lbarden.planningpoker.model.Player;
import de.lbarden.planningpoker.model.Room;
import org.springframework.stereotype.Service;

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
        }
    }

    public void updatePlayerCard(String roomId, String playerId, String card) {
        Room room = getRoom(roomId);
        if (room != null) {
            Player player = room.getPlayers().get(playerId);
            if (player != null) {
                player.setCard(card);
            }
        }
    }

    public void revealCards(String roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.setRevealed(true);
        }
    }

    public void resetRoom(String roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.setRevealed(false);
            room.setReset(true);
            room.getPlayers().values().forEach(player -> player.setCard(""));
        }
    }

    public void startRoom(String roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.setReset(false);
        }
    }

    public void removePlayer(String roomId, String playerId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.getPlayers().remove(playerId);
        }
    }
}
