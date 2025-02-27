package de.lbarden.planningpoker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Player {
    // Getters and setters
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

}
