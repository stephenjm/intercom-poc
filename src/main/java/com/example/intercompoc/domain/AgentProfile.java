package com.example.intercompoc.domain;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AgentProfile {
    @Id
    private Long id;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    private int activeConversationCount = 0;
    private int escalatedConversationCount = 0;
    private boolean isOnline = true;
    private LocalDateTime lastAssignedAt = LocalDateTime.now();
}
