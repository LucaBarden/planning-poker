package com.example.planningpoker.model;

import java.util.Collection;

public class PokerMessage {

    public enum MessageType {
        JOIN,
        CARD_PLAYED,
        REVEAL,
        RESET,
        UPDATE
    }

    private MessageType type;
    private String roomId;
    private String playerId;
    private String playerName;
    private String card;

    // Fields for broadcasting the updated room state:
    private Collection<Player> players;
    private boolean revealed;

    public PokerMessage() { }

    // Getters and setters
    public MessageType getType() {
        return type;
    }
    public void setType(MessageType type) {
        this.type = type;
    }
    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    public String getPlayerId() {
        return playerId;
    }
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public String getCard() {
        return card;
    }
    public void setCard(String card) {
        this.card = card;
    }
    public Collection<Player> getPlayers() {
        return players;
    }
    public void setPlayers(Collection<Player> players) {
        this.players = players;
    }
    public boolean isRevealed() {
        return revealed;
    }
    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }
}
