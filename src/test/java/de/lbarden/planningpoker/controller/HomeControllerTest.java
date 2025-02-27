package de.lbarden.planningpoker.controller;

import de.lbarden.planningpoker.model.Room;
import de.lbarden.planningpoker.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private RoomService roomService;
    
    @Autowired
    private HomeController homeController;

    @Test
    @DisplayName("Index Page gets displayed")
    public void testIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }


    @Test
    @DisplayName("Room Route redirects when room not found")
    void testRoomRoute_RedirectIfRoomNotFound() throws Exception {
        when(roomService.getRoom("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/room/nonexistent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    
    @Test
    @DisplayName("Create Room validates empty room names")
    void testCreateRoom_EmptyRoomName() throws Exception {
        mockMvc.perform(post("/createRoom")
                        .param("roomName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("error", "Room name cannot be empty"));
        
        verify(roomService, never()).createRoom(anyString());
    }

    @Test
    @DisplayName("Room Route uses existence cache for known nonexistent rooms")
    void testRoomRoute_UsesExistenceCache() throws Exception {
        // Set up the cache to know a room doesn't exist
        Field cacheField = ReflectionUtils.findField(HomeController.class, "roomExistenceCache");
        cacheField.setAccessible(true);
        ConcurrentMap<String, Boolean> cache = (ConcurrentMap<String, Boolean>) cacheField.get(homeController);
        cache.put("nonexistent", false);
        
        // Access the nonexistent room
        mockMvc.perform(get("/room/nonexistent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        
        // Verify RoomService was not called
        verify(roomService, never()).getRoom("nonexistent");
    }
    
    @Test
    @DisplayName("Admin prune cache removes nonexistent rooms")
    void testPruneCache() throws Exception {
        // Set up the cache with both existing and nonexistent rooms
        Field cacheField = ReflectionUtils.findField(HomeController.class, "roomExistenceCache");
        cacheField.setAccessible(true);
        ConcurrentMap<String, Boolean> cache = (ConcurrentMap<String, Boolean>) cacheField.get(homeController);
        
        cache.put("existing", true);
        cache.put("nonexistent1", true);
        cache.put("nonexistent2", false);
        
        // Mock RoomService to return room only for "existing"
        when(roomService.getRoom("existing")).thenReturn(new Room("existing", "Existing Room"));
        when(roomService.getRoom("nonexistent1")).thenReturn(null);
        when(roomService.getRoom("nonexistent2")).thenReturn(null);
        
        // Call the prune endpoint
        MvcResult result = mockMvc.perform(get("/admin/prune-cache"))
                .andExpect(status().isOk())
                .andReturn();
        
        // Verify result contains correct counts
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("3 -> 0"), "Should indicate 3 rooms before, 0 after");
        
        // Verify only the existing room remains in cache
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("existing"));
    }
}