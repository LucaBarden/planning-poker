package com.example.planningpoker.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Setter
@Getter
public class PokerMessage {

    public enum MessageType {
        JOIN,
        CARD_PLAYED,
        REVEAL,
        RESET,
        UPDATE,
        LEAVE
    }

    // Getters and setters
    private MessageType type;
    private String roomId;
    private String playerId;
    private String playerName;
    private String card;

    // Fields for broadcasting the updated room state:
    private Collection<Player> players;
    private boolean revealed;
    private boolean reset;

    public PokerMessage() { }

}
