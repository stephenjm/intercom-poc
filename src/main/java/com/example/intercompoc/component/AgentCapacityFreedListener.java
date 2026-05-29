package com.example.intercompoc.component;

import com.example.intercompoc.event.AgentCapacityFreedEvent;
import com.example.intercompoc.service.ConversationRoutingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AgentCapacityFreedListener {
    private static final Logger log = LoggerFactory.getLogger(AgentCapacityFreedListener.class);
    private final ConversationRoutingService routingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCapacityFreed(AgentCapacityFreedEvent event) {
        log.info("Event Received: Capacity freed for Agent {}. Processing backlog...", event.getAgentId());
        // Because we are calling this from an external bean, Spring's AOP proxy intercepts it
        // and properly applies the @Transactional(REQUIRES_NEW) annotation!
        routingService.processBacklog();
    }
}