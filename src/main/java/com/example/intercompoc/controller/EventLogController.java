package com.example.intercompoc.controller;

import com.example.intercompoc.dto.Dtos.EventLogRequest;
import com.example.intercompoc.service.EventLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class EventLogController {

    private final EventLogService eventLogService;

    public EventLogController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @GetMapping("/{id}/events")
    public List<EventLogRequest> getConversationEvents(@PathVariable Long id) {
        return eventLogService.getConversationEvents(id);
    }
}