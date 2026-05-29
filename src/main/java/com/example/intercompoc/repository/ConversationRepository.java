package com.example.intercompoc.repository;

import com.example.intercompoc.domain.Conversation;
import com.example.intercompoc.domain.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Conversation> findByAgentIdAndStatus(Long agentId, ConversationStatus status);
    List<Conversation> findByStatus(ConversationStatus status);
    java.util.Optional<com.example.intercompoc.domain.Conversation> findFirstByStatusOrderByCreatedAtAsc(com.example.intercompoc.domain.ConversationStatus status);
}
