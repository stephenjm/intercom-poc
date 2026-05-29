package com.example.intercompoc.service;

import com.example.intercompoc.domain.*;
import com.example.intercompoc.repository.*;
import com.example.intercompoc.event.AgentCapacityFreedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationRoutingService {
    private static final Logger log = LoggerFactory.getLogger(ConversationRoutingService.class);
    private final AgentProfileRepository agentProfileRepository;
    private final ConversationRepository conversationRepository;
    private final EventLogRepository eventLogRepository;

    @Transactional
    public Conversation route(Conversation conversation) {
        List<AgentProfile> availableAgents = agentProfileRepository.findAvailableAgents();
        if (!availableAgents.isEmpty()) {
            AgentProfile selected = availableAgents.get(0);
            conversation.setAgent(selected.getUser());
            conversation.setStatus(ConversationStatus.OPEN);
            conversationRepository.save(conversation);

            selected.setActiveConversationCount(selected.getActiveConversationCount() + 1);
            selected.setLastAssignedAt(LocalDateTime.now());
            agentProfileRepository.save(selected);

            EventLog eventLog = new EventLog();
            eventLog.setConversation(conversation);
            eventLog.setAction("ROUTED");
            eventLog.setDetails("Routed via ACD to agent " + selected.getUser().getId());
            eventLog.setTimestamp(LocalDateTime.now());
            eventLogRepository.save(eventLog);

            log.info("Assigned conversation {} to Agent {}", conversation.getId(), selected.getUser().getId());
        } else {
            conversation.setStatus(ConversationStatus.UNASSIGNED);
            conversationRepository.save(conversation);
            log.info("No agents available. Conversation {} remains UNASSIGNED.", conversation.getId());
        }
        return conversation;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBacklog() {
        log.info("Processing backlog queue due to freed capacity...");
        conversationRepository.findFirstByStatusOrderByCreatedAtAsc(ConversationStatus.UNASSIGNED)
            .ifPresent(this::route);
    }
}
