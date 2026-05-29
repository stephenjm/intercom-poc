package com.example.intercompoc.service;

import com.example.intercompoc.domain.*;
import com.example.intercompoc.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import com.example.intercompoc.repository.AgentProfileRepository;
import com.example.intercompoc.domain.AgentProfile;
import com.example.intercompoc.event.AgentCapacityFreedEvent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    private final ConversationRoutingService routingService;
    private final AgentProfileRepository agentProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    private final EventLogRepository eventLogRepository;

    public ConversationService(ConversationRepository conversationRepository, UserRepository userRepository, ConversationRoutingService routingService, AgentProfileRepository agentProfileRepository, ApplicationEventPublisher eventPublisher, EventLogRepository eventLogRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.routingService = routingService;
        this.agentProfileRepository = agentProfileRepository;
        this.eventPublisher = eventPublisher;
        this.eventLogRepository = eventLogRepository;
    }

    @Transactional
    public Conversation createConversation(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Conversation conv = new Conversation();
        conv.setCustomer(customer);
        conv.setCreatedAt(LocalDateTime.now());
        Conversation created = routingService.route(conv);

        logEvent(created, "CREATED", "Conversation opened by customer " + customer.getName());
        return created;
    }

    @Transactional
    public Conversation assignAgent(Long conversationId, Long targetAgentId, Long callerId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        User targetAgent = userRepository.findById(targetAgentId)
                .orElseThrow(() -> new IllegalArgumentException("Target Agent not found"));
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new IllegalArgumentException("Caller not found"));

        // Rule: Agents cannot reassign to other agents
        if ("AGENT".equals(caller.getRole()) && !caller.getId().equals(targetAgentId)) {
            throw new IllegalStateException("Agents cannot reassign to other agents. Please escalate to a manager.");
        }

        conv.setAgent(targetAgent);
        conv.setStatus(ConversationStatus.OPEN);
        conv = conversationRepository.save(conv);

        logEvent(conv, "ASSIGNED", "Assigned to " + targetAgent.getName() + " by " + caller.getName());
        return conv;
    }

    @Transactional
    public Conversation escalateToManager(Long conversationId, Long managerId, Long callerId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Target Manager not found"));
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new IllegalArgumentException("Caller not found"));

        if (!"MANAGER".equals(manager.getRole())) {
            throw new IllegalArgumentException("Target user is not a manager");
        }

        conv.setAgent(manager);
        conv.setStatus(ConversationStatus.ESCALATED);
        Conversation escalatedConv = conversationRepository.save(conv);

        AgentProfile profile = agentProfileRepository.findById(callerId).orElseThrow(() -> new IllegalStateException("Agent profile not found"));
        if(profile.getEscalatedConversationCount() >= 1) {
            throw new IllegalStateException("Agents can only have 1 escalated ticket at a time.");
        }
        profile.setActiveConversationCount(profile.getActiveConversationCount() - 1);
        profile.setEscalatedConversationCount(profile.getEscalatedConversationCount() + 1);
        agentProfileRepository.save(profile);

        // Fire event
        eventPublisher.publishEvent(new AgentCapacityFreedEvent(this, callerId));

        logEvent(escalatedConv, "ESCALATED", "Escalated to manager " + manager.getName() + " by " + caller.getName());
        return escalatedConv;
    }

    @Transactional
    public Conversation updateStatus(Long conversationId, ConversationStatus status) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conv.setStatus(status);
        conv = conversationRepository.save(conv);

        logEvent(conv, "STATUS_CHANGED", "Status changed to " + status.name());
        return conv;
    }

    private void logEvent(Conversation conv, String action, String details) {
        EventLog log = new EventLog();
        log.setConversation(conv);
        log.setAction(action);
        log.setDetails(details);
        eventLogRepository.save(log);
    }

    public List<Conversation> getConversationsForCustomer(Long customerId) {
        return conversationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
