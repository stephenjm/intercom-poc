package com.example.intercompoc.controller;

import com.example.intercompoc.domain.Message;
import com.example.intercompoc.dto.Dtos.SendMessageRequest;
import com.example.intercompoc.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public Message sendMessage(@PathVariable Long conversationId, @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(conversationId, request.getSenderId(), request.getContent());
    }

    @GetMapping
    public Page<Message> getMessages(@PathVariable Long conversationId, 
                                     @RequestParam(defaultValue = "0") int page) {
        return messageService.getChatHistory(conversationId, page);
    }
}
