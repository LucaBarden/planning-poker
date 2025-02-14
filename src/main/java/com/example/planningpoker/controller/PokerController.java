package com.example.planningpoker.controller;

import com.example.planningpoker.model.PokerMessage;
import com.example.planningpoker.model.PokerMessage.MessageType;
import com.example.planningpoker.model.Player;
import com.example.planningpoker.model.Room;
import com.example.planningpoker.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class PokerController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/room")
    public void handleRoomMessage(@Payload PokerMessage message) {
        String roomId = message.getRoomId();
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            return;
        }

        switch (message.getType()) {
            case JOIN:
                // Assign a random player ID if one isn’t already provided
                if (message.getPlayerId() == null || message.getPlayerId().isEmpty()) {
                    message.setPlayerId(UUID.randomUUID().toString());
                }
                Player newPlayer = new Player(message.getPlayerId(), message.getPlayerName());
                roomService.addPlayer(roomId, newPlayer);
                break;
            case CARD_PLAYED:
                // Update the player’s card selection
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
            default:
                break;
        }

        // Prepare an update message with the room’s current state
        PokerMessage updateMessage = new PokerMessage();
        updateMessage.setType(MessageType.UPDATE);
        updateMessage.setRoomId(roomId);
        updateMessage.setPlayers(room.getPlayerList());
        updateMessage.setRevealed(room.isRevealed());
        updateMessage.setReset(room.isReset());
        roomService.startRoom(roomId);

        // Broadcast the update only to the clients in this room
        messagingTemplate.convertAndSend("/topic/room/" + roomId, updateMessage);

    }
}
