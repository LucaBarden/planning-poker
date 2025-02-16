package de.lbarden.planningpoker.controller;

import de.lbarden.planningpoker.model.Room;
import de.lbarden.planningpoker.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // Create a new room with a provided name and redirect to its URL
    @PostMapping("/createRoom")
    public String createRoom(@RequestParam("roomName") String roomName, Model model) {
        Room room = roomService.createRoom(roomName);
        return "redirect:/room/" + room.getId();
    }

    // Render the room page for the given roomId
    @GetMapping("/room/{roomId}")
    public String room(@PathVariable("roomId") String roomId, Model model) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            return "redirect:/";
        }
        model.addAttribute("roomId", roomId);
        model.addAttribute("roomName", room.getName());
        return "room";
    }
}
