package com.example.intercompoc.repository;
import com.example.intercompoc.domain.AgentProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentProfile a WHERE a.isOnline = true AND a.activeConversationCount < 3 ORDER BY a.activeConversationCount ASC, a.lastAssignedAt ASC")
    List<AgentProfile> findAvailableAgents();
}
