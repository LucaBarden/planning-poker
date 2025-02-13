package com.example.planningpoker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Player {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("card")
    private String card;

    public Player() { }

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.card = "";
    }

    // Getters and setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getCard() {
        return card;
    }
    public void setCard(String card) {
        this.card = card;
    }
}
