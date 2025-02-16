package de.lbarden.planningpoker.controller;


import de.lbarden.planningpoker.model.Room;
import de.lbarden.planningpoker.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
public class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @Test
    @DisplayName("Index Page gets displayed")
    public void testIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testRoomRoute_ModelAttributesPresent() throws Exception {
        // Assume a Room constructor: new Room(String id, String name)
        Room room = new Room("room-123", "Test Room");
        when(roomService.getRoom("room-123")).thenReturn(room);

        mockMvc.perform(get("/room/room-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("room"))
                .andExpect(model().attribute("roomId", "room-123"))
                .andExpect(model().attribute("roomName", "Test Room"));
    }

    @Test
    void testRoomRoute_RedirectIfRoomNotFound() throws Exception {
        when(roomService.getRoom("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/room/nonexistent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testCreateRoomRoute() throws Exception {
        // Simulate room creation via POST /createRoom with parameter roomName=New Room
        Room room = new Room("room-456", "New Room");
        when(roomService.createRoom("New Room")).thenReturn(room);

        mockMvc.perform(post("/createRoom")
                        .param("roomName", "New Room"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/room/room-456"));
    }
}
