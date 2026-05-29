package com.example.intercompoc.repository;

import com.example.intercompoc.domain.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByConversationIdOrderByTimestampDesc(Long conversationId);
}
