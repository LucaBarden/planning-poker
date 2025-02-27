package de.lbarden.planningpoker.controller;

import de.lbarden.planningpoker.model.PokerMessage;
import de.lbarden.planningpoker.model.PokerMessage.MessageType;
import de.lbarden.planningpoker.model.Player;
import de.lbarden.planningpoker.model.Room;
import de.lbarden.planningpoker.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class PokerController {
    private static final Logger logger = LoggerFactory.getLogger(PokerController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/room")
    public void handleRoomMessage(@Payload PokerMessage message) {
        String roomId = message.getRoomId();
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            logger.warn("Message received for non-existent room: {}", roomId);
            return;
        }

        try {
            switch (message.getType()) {
                case JOIN:
                    // Assign a random player ID if one isn't already provided
                    if (message.getPlayerId() == null || message.getPlayerId().isEmpty()) {
                        message.setPlayerId(UUID.randomUUID().toString());
                    }
                    Player newPlayer = new Player(message.getPlayerId(), message.getPlayerName());
                    roomService.addPlayer(roomId, newPlayer);
                    logger.debug("Player joined: {} ({})", message.getPlayerName(), message.getPlayerId());
                    break;
                case CARD_PLAYED:
                    // Update the player's card selection
                    roomService.updatePlayerCard(roomId, message.getPlayerId(), message.getCard());
                    break;
                case REVEAL:
                    // Reveal all cards in the room
                    roomService.revealCards(roomId);
                    break;
                case RESET:
                    // Reset the room (clear cards and hide them)
                    roomService.resetRoom(roomId);
                    break;
                case LEAVE:
                    roomService.removePlayer(roomId, message.getPlayerId());
                    logger.debug("Player left: {}", message.getPlayerId());
                    break; // Fixed: Added missing break statement
                default:
                    logger.warn("Unknown message type received: {}", message.getType());
                    break;
            }

            // Reuse the incoming message object to avoid creating a new one
            message.setType(MessageType.UPDATE);
            message.setPlayers(room.getPlayerList());
            message.setRevealed(room.isRevealed());
            message.setReset(room.isReset());
            roomService.startRoom(roomId);

            // Broadcast the update only to the clients in this room
            messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        } catch (Exception e) {
            // Log exception and return a graceful error to the client
            logger.error("Error processing message: {}", e.getMessage(), e);
            
            PokerMessage errorMessage = new PokerMessage();
            errorMessage.setType(MessageType.UPDATE); // Still use UPDATE to ensure client handling
            errorMessage.setRoomId(roomId);
            
            if (room != null) {
                errorMessage.setPlayers(room.getPlayerList());
                errorMessage.setRevealed(room.isRevealed());
                errorMessage.setReset(room.isReset());
            }
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId, errorMessage);
        }
    }
}