package com.example.intercompoc.dto;

import com.example.intercompoc.domain.ConversationStatus;
import lombok.Data;

import java.time.LocalDateTime;

public class Dtos {
    @Data
    public static class CreateConversationRequest {
        private Long customerId;
    }

    @Data
    public static class AssignAgentRequest {
        private Long agentId;
    }

    @Data
    public static class EscalateRequest {
        private Long managerId;
    }

    @Data
    public static class UpdateStatusRequest {
        private ConversationStatus status;
    }

    @Data
    public static class SendMessageRequest {
        private Long senderId;
        private String content;
    }

    @Data
    public static class EventLogRequest {
        private Long id;
        private String action;
        private String details;
        private LocalDateTime timestamp;
    }
}
