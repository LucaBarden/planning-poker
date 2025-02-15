package de.lbarden.planningpoker.service;

import de.lbarden.planningpoker.model.Player;
import de.lbarden.planningpoker.model.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RoomServiceTest {

    private final RoomService roomService = new RoomService();

    @Test
    @DisplayName("Room Service correctly creates a room")
    public void testCreateRoom() {
        Room room = roomService.createRoom("Test Room");
        assertNotNull(room);
        assertNotNull(room.getId());
        assertEquals("Test Room", room.getName());
    }

    @Test
    @DisplayName("Players get correctly added to rooms")
    public void testAddPlayer() {
        Room room = roomService.createRoom("Test Room");
        Player player = new Player("player-1", "Alice");
        roomService.addPlayer(room.getId(), player);
        Room fetchedRoom = roomService.getRoom(room.getId());
        assertNotNull(fetchedRoom);
        assertTrue(fetchedRoom.getPlayers().containsKey("player-1"));
    }

    @Test
    @DisplayName("A played card is correctly saved to the user")
    public void testUpdatePlayerCard() {
        Room room = roomService.createRoom("Test Room");
        Player player = new Player("player-1", "Alice");
        roomService.addPlayer(room.getId(), player);
        roomService.updatePlayerCard(room.getId(), "player-1", "5");
        Room fetchedRoom = roomService.getRoom(room.getId());
        assertEquals("5", fetchedRoom.getPlayers().get("player-1").getCard());
    }

    @Test
    @DisplayName("Room Reveal is correctly saved")
    public void testRevealCards() {
        Room room = roomService.createRoom("Test Room");
        Player player = new Player("player-1", "Alice");
        roomService.addPlayer(room.getId(), player);
        roomService.revealCards(room.getId());
        Room fetchedRoom = roomService.getRoom(room.getId());
        assertTrue(fetchedRoom.isRevealed());
    }

    @Test
    @DisplayName("Room Reset works as intended")
    public void testResetRoom() {
        Room room = roomService.createRoom("Test Room");
        Player player = new Player("player-1", "Alice");
        roomService.addPlayer(room.getId(), player);
        roomService.updatePlayerCard(room.getId(), "player-1", "8");
        roomService.revealCards(room.getId());
        roomService.resetRoom(room.getId());
        Room fetchedRoom = roomService.getRoom(room.getId());
        assertFalse(fetchedRoom.isRevealed());
        assertEquals("", fetchedRoom.getPlayers().get("player-1").getCard());
    }
}
