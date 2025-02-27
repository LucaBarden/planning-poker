package de.lbarden.planningpoker.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    @DisplayName("Room initializes with correct values")
    void testRoomInitialization() {
        Room room = new Room("test-id", "Test Room");
        
        assertEquals("test-id", room.getId());
        assertEquals("Test Room", room.getName());
        assertFalse(room.isRevealed());
        assertFalse(room.isReset());
        assertNotNull(room.getPlayers());
        assertTrue(room.getPlayers().isEmpty());
        assertTrue(room.getLastActivity() > 0);
    }
    
    @Test
    @DisplayName("UpdateLastActivity updates timestamp")
    void testUpdateLastActivity() throws InterruptedException {
        Room room = new Room("test-id", "Test Room");
        long initialActivity = room.getLastActivity();
        
        // Wait a bit to ensure time difference
        Thread.sleep(10);
        
        room.updateLastActivity();
        long updatedActivity = room.getLastActivity();
        
        assertTrue(updatedActivity > initialActivity);
    }
    
    @Test
    @DisplayName("Adding player works correctly")
    void testAddPlayer() {
        Room room = new Room("test-id", "Test Room");
        Player player = new Player("player-1", "Alice");
        
        room.addPlayer(player.getId(), player);
        
        assertTrue(room.getPlayers().containsKey("player-1"));
        assertEquals(player, room.getPlayers().get("player-1"));
    }
    
    @Test
    @DisplayName("Removing player works correctly")
    void testRemovePlayer() {
        Room room = new Room("test-id", "Test Room");
        Player player = new Player("player-1", "Alice");
        
        room.addPlayer(player.getId(), player);
        assertTrue(room.getPlayers().containsKey("player-1"));
        
        Player removed = room.removePlayer("player-1");
        
        assertFalse(room.getPlayers().containsKey("player-1"));
        assertEquals(player, removed);
    }
    
    @Test
    @DisplayName("Removing nonexistent player returns null")
    void testRemoveNonexistentPlayer() {
        Room room = new Room("test-id", "Test Room");
        
        Player removed = room.removePlayer("nonexistent");
        
        assertNull(removed);
    }
    
    @Test
    @DisplayName("GetPlayerList returns cached collection")
    void testGetPlayerList_ReturnsCachedCollection() {
        Room room = new Room("test-id", "Test Room");
        Player player1 = new Player("player-1", "Alice");
        Player player2 = new Player("player-2", "Bob");
        
        room.addPlayer(player1.getId(), player1);
        room.addPlayer(player2.getId(), player2);
        
        // Get the player list twice
        Collection<Player> playerList1 = room.getPlayerList();
        Collection<Player> playerList2 = room.getPlayerList();
        
        // They should be the same instance
        assertSame(playerList1, playerList2);
        
        // And have the correct content
        assertEquals(2, playerList1.size());
        assertTrue(playerList1.contains(player1));
        assertTrue(playerList1.contains(player2));
    }
    
    @Test
    @DisplayName("Cache is invalidated when player is added")
    void testCacheInvalidation_PlayerAdded() {
        Room room = new Room("test-id", "Test Room");
        Player player1 = new Player("player-1", "Alice");
        
        // Add first player and get list
        room.addPlayer(player1.getId(), player1);
        Collection<Player> playerList1 = room.getPlayerList();
        
        // Add second player and get list again
        Player player2 = new Player("player-2", "Bob");
        room.addPlayer(player2.getId(), player2);
        Collection<Player> playerList2 = room.getPlayerList();
        
        // The lists should be different instances
        assertNotSame(playerList1, playerList2);
        
        // And the second list should have both players
        assertEquals(2, playerList2.size());
        assertTrue(playerList2.contains(player1));
        assertTrue(playerList2.contains(player2));
    }
    
    @Test
    @DisplayName("Cache is invalidated when player is removed")
    void testCacheInvalidation_PlayerRemoved() {
        Room room = new Room("test-id", "Test Room");
        Player player1 = new Player("player-1", "Alice");
        Player player2 = new Player("player-2", "Bob");
        
        // Add players and get list
        room.addPlayer(player1.getId(), player1);
        room.addPlayer(player2.getId(), player2);
        Collection<Player> playerList1 = room.getPlayerList();
        
        // Remove a player and get list again
        room.removePlayer(player1.getId());
        Collection<Player> playerList2 = room.getPlayerList();
        
        // The lists should be different instances
        assertNotSame(playerList1, playerList2);
        
        // And the second list should have only the remaining player
        assertEquals(1, playerList2.size());
        assertTrue(playerList2.contains(player2));
    }
    
    @Test
    @DisplayName("Player list is unmodifiable")
    void testPlayerListIsUnmodifiable() {
        Room room = new Room("test-id", "Test Room");
        Player player = new Player("player-1", "Alice");
        room.addPlayer(player.getId(), player);
        
        Collection<Player> playerList = room.getPlayerList();
        
        // Trying to modify the list should throw exception
        assertThrows(UnsupportedOperationException.class, () -> {
            playerList.add(new Player("player-2", "Bob"));
        });
    }
    
    @Test
    @DisplayName("Player list thread safety")
    void testPlayerListThreadSafety() throws Exception {
        final Room room = new Room("test-id", "Test Room");
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> exception = new AtomicReference<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Create threads that will concurrently modify and read the player list
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // Add a player
                    String playerId = "player-" + threadId;
                    Player player = new Player(playerId, "Player " + threadId);
                    room.addPlayer(playerId, player);
                    
                    // Get the player list
                    Collection<Player> playerList = room.getPlayerList();
                    
                    // Remove the player
                    room.removePlayer(playerId);
                    
                    // Get the player list again
                    Collection<Player> updatedList = room.getPlayerList();
                    
                    // Verify player isn't in updated list
                    assertFalse(updatedList.contains(player));
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads
        startLatch.countDown();
        
        // Wait for all threads to finish
        doneLatch.await();
        executor.shutdown();
        
        // Check if any thread had an exception
        if (exception.get() != null) {
            throw exception.get();
        }
    }
}