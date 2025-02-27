package de.lbarden.planningpoker.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    @DisplayName("Player initializes with correct values")
    void testPlayerInitialization() {
        Player player = new Player("player-1", "Alice");
        
        assertEquals("player-1", player.getId());
        assertEquals("Alice", player.getName());
        assertEquals("", player.getCard());
    }
    
    @Test
    @DisplayName("Player empty constructor works")
    void testEmptyConstructor() {
        Player player = new Player();
        
        assertNull(player.getId());
        assertNull(player.getName());
        assertNull(player.getCard());
    }
    
    @Test
    @DisplayName("Player setters work correctly")
    void testSetters() {
        Player player = new Player();
        
        player.setId("player-id");
        player.setName("Bob");
        player.setCard("8");
        
        assertEquals("player-id", player.getId());
        assertEquals("Bob", player.getName());
        assertEquals("8", player.getCard());
    }
    
    @Test
    @DisplayName("Player equals and hashCode work")
    void testEqualsAndHashCode() {
        Player player1 = new Player("player-1", "Alice");
        Player player2 = new Player("player-1", "Alice");
        Player player3 = new Player("player-2", "Bob");
        
        // Equals is typically based on ID for entity objects
        assertEquals(player1, player2);
        assertNotEquals(player1, player3);
        
        // HashCode should be consistent with equals
        assertEquals(player1.hashCode(), player2.hashCode());
        assertNotEquals(player1.hashCode(), player3.hashCode());
    }
}