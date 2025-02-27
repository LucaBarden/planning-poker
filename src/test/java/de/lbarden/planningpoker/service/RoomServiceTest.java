package de.lbarden.planningpoker.service;

import de.lbarden.planningpoker.model.Player;
import de.lbarden.planningpoker.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @InjectMocks
    private RoomService roomService;

    @Spy
    private Map<String, Room> roomsMap = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        // Inject the spy map into the roomService using reflection
        ReflectionTestUtils.setField(roomService, "rooms", roomsMap);
    }

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

    @Test
    @DisplayName("Start Room updates reset flag")
    void testStartRoom() {
        Room room = roomService.createRoom("Test Room");
        room.setReset(true);
        roomService.startRoom(room.getId());
        Room updatedRoom = roomService.getRoom(room.getId());
        assertFalse(updatedRoom.isReset(), "Room should be not reset after startRoom is called");
    }

    @Test
    @DisplayName("Remove Player removes player from room")
    void testRemovePlayer() {
        Room room = roomService.createRoom("Test Room");
        Player player = new Player("player-1", "Alice");
        roomService.addPlayer(room.getId(), player);
        assertTrue(room.getPlayers().containsKey("player-1"), "Player should exist in room");

        roomService.removePlayer(room.getId(), "player-1");
        assertFalse(room.getPlayers().containsKey("player-1"), "Player should be removed from room");
    }
    
    @Test
    @DisplayName("Remove Player removes empty room")
    void testRemovePlayer_RemovesEmptyRoom() {
        Room room = roomService.createRoom("Test Room");
        Player player = new Player("player-1", "Alice");
        roomService.addPlayer(room.getId(), player);
        
        // Before removing the only player, room should exist
        assertNotNull(roomService.getRoom(room.getId()));
        
        // After removing the only player, room should be removed
        roomService.removePlayer(room.getId(), "player-1");
        assertNull(roomService.getRoom(room.getId()));
    }
    
    @Test
    @DisplayName("Room cache is used for frequently accessed rooms")
    void testRoomCache() {
        // Create a room and access it to cache it
        Room room = roomService.createRoom("Test Room");
        String roomId = room.getId();
        
        // Get the room to cache it
        Room cachedRoom = roomService.getRoom(roomId);
        assertNotNull(cachedRoom);
        
        // Verify that the same instance is returned from cache
        assertSame(room, cachedRoom, "The same room instance should be returned from cache");
        
        // Spy the rooms map to verify it's not accessed when using cache
        verify(roomsMap, times(0)).get(roomId);
    }
    
    @Test
    @DisplayName("Remove Stale Rooms removes inactive rooms")
    void testRemoveStaleRooms() {
        // Create a room and set its last activity 61 minutes ago
        Room staleRoom = new Room("stale-room", "Stale Room");
        long staleThreshold = TimeUnit.MINUTES.toMillis(60);
        staleRoom.setLastActivity(System.currentTimeMillis() - (staleThreshold + 1000)); // 61 minutes ago
        
        // Add room directly to the map
        roomsMap.put(staleRoom.getId(), staleRoom);
        
        // Create a fresh room
        Room freshRoom = roomService.createRoom("Fresh Room");
        
        // Verify both rooms exist
        assertEquals(2, roomsMap.size());
        
        // Run the cleanup
        roomService.removeStaleRooms();
        
        // Verify only the fresh room exists
        assertEquals(1, roomsMap.size());
        assertNull(roomService.getRoom(staleRoom.getId()), "Stale room should be removed");
        assertNotNull(roomService.getRoom(freshRoom.getId()), "Fresh room should remain");
    }
    
    @Test
    @DisplayName("Remove Oldest Rooms when capacity limit is reached")
    void testRemoveOldestRooms() throws Exception {
        // Create MAX_ROOMS + 1 rooms with increasing last activity times
        int maxRooms = (int) ReflectionTestUtils.getField(roomService, "MAX_ROOMS");
        
        // Mock a smaller MAX_ROOMS for testing
        ReflectionTestUtils.setField(roomService, "MAX_ROOMS", 10);
        
        // Create 11 rooms with increasing activity times
        for (int i = 0; i < 11; i++) {
            Room room = roomService.createRoom("Room " + i);
            room.setLastActivity(System.currentTimeMillis() + (i * 1000)); // Each room is newer
            Thread.sleep(10); // Ensure different timestamps
        }
        
        // Trying to create one more room should trigger cleanup
        Room newRoom = roomService.createRoom("New Room");
        
        // We should have max 10 rooms, and the oldest should be removed
        assertTrue(roomsMap.size() <= 10, "Room count should be at most 10");
        assertNotNull(roomService.getRoom(newRoom.getId()), "Newest room should exist");
        assertNull(roomService.getRoom("Room 0"), "Oldest room should be removed");
        
        // Reset MAX_ROOMS
        ReflectionTestUtils.setField(roomService, "MAX_ROOMS", maxRooms);
    }
    
    @Test
    @DisplayName("Get Room handles null roomId gracefully")
    void testGetRoom_WithNullId() {
        Room room = roomService.getRoom(null);
        assertNull(room, "Null room ID should return null");
    }
    
    @Test
    @DisplayName("Methods handle nonexistent room IDs gracefully")
    void testMethodsWithNonexistentRoom() {
        String nonexistentId = "nonexistent";
        
        // These should not throw exceptions
        roomService.addPlayer(nonexistentId, new Player("player-1", "Alice"));
        roomService.updatePlayerCard(nonexistentId, "player-1", "5");
        roomService.revealCards(nonexistentId);
        roomService.resetRoom(nonexistentId);
        roomService.startRoom(nonexistentId);
        roomService.removePlayer(nonexistentId, "player-1");
        
        // Verify no rooms were created
        assertTrue(roomsMap.isEmpty(), "No rooms should be created");
    }
}