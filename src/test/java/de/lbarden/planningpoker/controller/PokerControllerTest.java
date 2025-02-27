package de.lbarden.planningpoker.controller;

import de.lbarden.planningpoker.model.PokerMessage;
import de.lbarden.planningpoker.model.PokerMessage.MessageType;
import de.lbarden.planningpoker.model.Player;
import de.lbarden.planningpoker.model.Room;
import de.lbarden.planningpoker.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PokerControllerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PokerController pokerController;

    private Room testRoom;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Assume the Room constructor takes an id and name
        testRoom = new Room("testRoom", "Test Room");
        when(roomService.getRoom("testRoom")).thenReturn(testRoom);
    }

    @Test
    @DisplayName("Test JOIN message handling")
    void testJoinMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.JOIN);
        message.setRoomId("testRoom");
        message.setPlayerId("player1");
        message.setPlayerName("Alice");

        pokerController.handleRoomMessage(message);

        // Verify that addPlayer was called
        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(roomService).addPlayer(eq("testRoom"), playerCaptor.capture());
        Player addedPlayer = playerCaptor.getValue();
        assertEquals("player1", addedPlayer.getId());
        assertEquals("Alice", addedPlayer.getName());

        // Verify that an update message is sent
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }

    @Test
    @DisplayName("Test CARD_PLAYED message handling")
    void testCardPlayedMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.CARD_PLAYED);
        message.setRoomId("testRoom");
        message.setPlayerId("player1");
        message.setCard("5");

        pokerController.handleRoomMessage(message);

        verify(roomService).updatePlayerCard("testRoom", "player1", "5");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }

    @Test
    @DisplayName("Test REVEAL message handling")
    void testRevealMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.REVEAL);
        message.setRoomId("testRoom");

        pokerController.handleRoomMessage(message);

        verify(roomService).revealCards("testRoom");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }

    @Test
    @DisplayName("Test RESET message handling")
    void testResetMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.RESET);
        message.setRoomId("testRoom");

        pokerController.handleRoomMessage(message);

        verify(roomService).resetRoom("testRoom");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }

    @Test
    @DisplayName("Test LEAVE message handling")
    void testLeaveMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.LEAVE);
        message.setRoomId("testRoom");
        message.setPlayerId("player1");

        pokerController.handleRoomMessage(message);

        verify(roomService).removePlayer("testRoom", "player1");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }
    
    @Test
    @DisplayName("Test JOIN message with missing player ID")
    void testJoinMessage_MissingPlayerId() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.JOIN);
        message.setRoomId("testRoom");
        message.setPlayerName("Alice");
        // PlayerId is intentionally not set
        
        pokerController.handleRoomMessage(message);
        
        // Verify that a player ID is generated
        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(roomService).addPlayer(eq("testRoom"), playerCaptor.capture());
        Player addedPlayer = playerCaptor.getValue();
        
        assertNotNull(addedPlayer.getId());
        assertFalse(addedPlayer.getId().isEmpty());
        assertEquals("Alice", addedPlayer.getName());
    }
    
    @Test
    @DisplayName("Test message with nonexistent room")
    void testMessage_NonexistentRoom() {
        // Room "nonexistent" does not exist
        when(roomService.getRoom("nonexistent")).thenReturn(null);
        
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.JOIN);
        message.setRoomId("nonexistent");
        message.setPlayerName("Alice");
        
        // Should not throw exception
        pokerController.handleRoomMessage(message);
        
        // Verify no service methods were called
        verify(roomService, never()).addPlayer(anyString(), any(Player.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(PokerMessage.class));
    }
    
    @Test
    @DisplayName("Test unknown message type handling")
    void testUnknownMessageType() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.UPDATE); // UPDATE is for internal use
        message.setRoomId("testRoom");
        
        pokerController.handleRoomMessage(message);
        
        // Verify the service call was made to update the client
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
        
        // But no room service methods were called
        verify(roomService, never()).addPlayer(anyString(), any(Player.class));
        verify(roomService, never()).updatePlayerCard(anyString(), anyString(), anyString());
        verify(roomService, never()).revealCards(anyString());
        verify(roomService, never()).resetRoom(anyString());
        verify(roomService, never()).removePlayer(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Test error handling when exception occurs")
    void testErrorHandling() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.CARD_PLAYED);
        message.setRoomId("testRoom");
        message.setPlayerId("player1");
        
        // Setup room players collection
        Collection<Player> players = Collections.singletonList(new Player("player1", "Alice"));
        testRoom.setRevealed(true);
        when(testRoom.getPlayerList()).thenReturn(players);
        
        // Make updatePlayerCard throw an exception
        doThrow(new RuntimeException("Test exception")).when(roomService)
            .updatePlayerCard(anyString(), anyString(), anyString());
        
        // Should not throw exception
        pokerController.handleRoomMessage(message);
        
        // Verify error message was sent
        ArgumentCaptor<PokerMessage> messageCaptor = ArgumentCaptor.forClass(PokerMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), messageCaptor.capture());
        
        PokerMessage errorMessage = messageCaptor.getValue();
        assertEquals(MessageType.UPDATE, errorMessage.getType());
        assertEquals("testRoom", errorMessage.getRoomId());
        assertEquals(players, errorMessage.getPlayers());
        assertTrue(errorMessage.isRevealed());
    }
    
    @Test
    @DisplayName("Test message reuse in controller")
    void testMessageReuse() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.REVEAL);
        message.setRoomId("testRoom");
        
        // Setup room players collection
        Collection<Player> players = Collections.singletonList(new Player("player1", "Alice"));
        when(testRoom.getPlayerList()).thenReturn(players);
        
        pokerController.handleRoomMessage(message);
        
        // Verify the same message object is reused
        ArgumentCaptor<PokerMessage> messageCaptor = ArgumentCaptor.forClass(PokerMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), messageCaptor.capture());
        
        PokerMessage sentMessage = messageCaptor.getValue();
        assertSame(message, sentMessage, "The same message object should be reused");
        assertEquals(MessageType.UPDATE, sentMessage.getType());
    }
}