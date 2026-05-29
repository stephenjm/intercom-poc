package com.example.intercompoc.controller;

import com.example.intercompoc.domain.Conversation;
import com.example.intercompoc.dto.Dtos.*;
import com.example.intercompoc.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public Conversation createConversation(@RequestBody CreateConversationRequest request) {
        return conversationService.createConversation(request.getCustomerId());
    }

    @GetMapping("/customer/{customerId}")
    public List<Conversation> getCustomerConversations(@PathVariable Long customerId) {
        return conversationService.getConversationsForCustomer(customerId);
    }

    @PatchMapping("/{id}/assign")
    public Conversation assignAgent(
            @PathVariable Long id,
            @RequestBody AssignAgentRequest request,
            @RequestHeader("X-Caller-Id") Long callerId) {
        return conversationService.assignAgent(id, request.getAgentId(), callerId);
    }

    @PatchMapping("/{id}/escalate")
    public Conversation escalateToManager(
            @PathVariable Long id,
            @RequestBody EscalateRequest request,
            @RequestHeader("X-Caller-Id") Long callerId) {
        return conversationService.escalateToManager(id, request.getManagerId(), callerId);
    }

    @PatchMapping("/{id}/status")
    public Conversation updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        return conversationService.updateStatus(id, request.getStatus());
    }
}