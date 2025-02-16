package de.lbarden.planningpoker.controller;

import de.lbarden.planningpoker.model.PokerMessage;
import de.lbarden.planningpoker.model.PokerMessage.MessageType;
import de.lbarden.planningpoker.model.Player;
import de.lbarden.planningpoker.model.Room;
import de.lbarden.planningpoker.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
    void testRevealMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.REVEAL);
        message.setRoomId("testRoom");

        pokerController.handleRoomMessage(message);

        verify(roomService).revealCards("testRoom");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }

    @Test
    void testResetMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.RESET);
        message.setRoomId("testRoom");

        pokerController.handleRoomMessage(message);

        verify(roomService).resetRoom("testRoom");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }

    @Test
    void testLeaveMessage() {
        PokerMessage message = new PokerMessage();
        message.setType(MessageType.LEAVE);
        message.setRoomId("testRoom");
        message.setPlayerId("player1");

        pokerController.handleRoomMessage(message);

        verify(roomService).removePlayer("testRoom", "player1");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/testRoom"), any(PokerMessage.class));
    }
}
