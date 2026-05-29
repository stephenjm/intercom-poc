package com.example.intercompoc.service;

import com.example.intercompoc.domain.EventLog;
import com.example.intercompoc.dto.Dtos.EventLogRequest;
import com.example.intercompoc.repository.EventLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventLogService {

    private final EventLogRepository eventLogRepository;

    public EventLogService(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    public List<EventLogRequest> getConversationEvents(Long conversationId) {
        return eventLogRepository.findByConversationIdOrderByTimestampDesc(conversationId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private EventLogRequest mapToDto(EventLog eventLog) {
        EventLogRequest dto = new EventLogRequest();
        dto.setId(eventLog.getId());
        dto.setAction(eventLog.getAction());
        dto.setDetails(eventLog.getDetails());
        dto.setTimestamp(eventLog.getTimestamp());
        return dto;
    }
}