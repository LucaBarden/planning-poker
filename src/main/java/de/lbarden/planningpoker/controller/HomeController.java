package de.lbarden.planningpoker.controller;

import de.lbarden.planningpoker.model.Room;
import de.lbarden.planningpoker.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Controller
public class HomeController {
    // Simple in-memory cache to avoid repeated lookups
    private final ConcurrentMap<String, Boolean> roomExistenceCache = new ConcurrentHashMap<>();
    private static final int MAX_ROOM_NAME_LENGTH = 50;

    @Autowired
    private RoomService roomService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // Create a new room with a provided name and redirect to its URL
    @PostMapping("/createRoom")
    public String createRoom(@RequestParam("roomName") String roomName, Model model) {
        // Input validation
        if (roomName == null || roomName.trim().isEmpty()) {
            model.addAttribute("error", "Room name cannot be empty");
            return "index";
        }
        
        // Trim and limit room name length
        roomName = roomName.trim();
        if (roomName.length() > MAX_ROOM_NAME_LENGTH) {
            roomName = roomName.substring(0, MAX_ROOM_NAME_LENGTH);
        }
        
        // Create room
        Room room = roomService.createRoom(roomName);
        
        // Store in existence cache
        roomExistenceCache.put(room.getId(), Boolean.TRUE);
        
        // Optionally store roomId in session to track user's rooms
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getSession(true);
        session.setAttribute("lastRoomId", room.getId());
        
        return "redirect:/room/" + room.getId();
    }

    // Render the room page for the given roomId
    @GetMapping("/room/{roomId}")
    public String room(@PathVariable("roomId") String roomId, Model model) {
        // First check the cache to see if we know this room doesn't exist
        Boolean exists = roomExistenceCache.get(roomId);
        if (exists != null && !exists) {
            return "redirect:/";
        }
        
        // Cache miss or room might exist - check with the service
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            // Update cache to remember this room doesn't exist
            roomExistenceCache.put(roomId, Boolean.FALSE);
            return "redirect:/";
        }
        
        // Update cache if needed
        if (exists == null) {
            roomExistenceCache.put(roomId, Boolean.TRUE);
        }
        
        model.addAttribute("roomId", roomId);
        model.addAttribute("roomName", room.getName());
        return "room";
    }
    
    // Method to prune the room existence cache periodically
    @GetMapping("/admin/prune-cache")
    @ResponseBody
    public String pruneCache() {
        int sizeBefore = roomExistenceCache.size();
        
        // Only keep cache entries for rooms that actually exist
        roomExistenceCache.keySet().forEach(id -> {
            if (roomService.getRoom(id) == null) {
                roomExistenceCache.remove(id);
            }
        });
        
        int sizeAfter = roomExistenceCache.size();
        return "Cache pruned: " + sizeBefore + " -> " + sizeAfter;
    }
}