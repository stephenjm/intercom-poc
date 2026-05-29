package com.example.intercompoc.event;

import org.springframework.context.ApplicationEvent;

public class AgentCapacityFreedEvent extends ApplicationEvent {
    private final Long agentId;
    public AgentCapacityFreedEvent(Object source, Long agentId) {
        super(source);
        this.agentId = agentId;
    }
    public Long getAgentId() { return agentId; }
}
