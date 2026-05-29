package com.example.intercompoc;

import com.example.intercompoc.domain.*;
import com.example.intercompoc.dto.Dtos.*;
import com.example.intercompoc.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
public class ConversationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private EventLogRepository eventLogRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private AgentProfileRepository agentProfileRepository;

    private Long customerId;
    private Long agentId;
    private Long managerId;
    private Long agent2Id;

    @BeforeEach
    void setup() {
        messageRepository.deleteAll();
        eventLogRepository.deleteAll();
        conversationRepository.deleteAll();
        agentProfileRepository.deleteAll();
        userRepository.deleteAll();

        User customer = new User(); customer.setName("Neo"); customer.setEmail(
                "neo@example.com"); customer.setRole("CUSTOMER");
        customer = userRepository.save(customer);
        customerId = customer.getId();

        User agent = new User(); agent.setName("Smith"); agent.setEmail("smith@intercom" +
                ".com"); agent.setRole("AGENT");


        User manager = new User(); manager.setName("Morpheus"); manager.setEmail(
                "morpheus@intercom.com"); manager.setRole("MANAGER");
        manager = userRepository.save(manager);
        managerId = manager.getId();

        User agent2 = new User(); agent2.setName("Upgrade"); agent2.setEmail("upgrade" +
                "@intercom.com"); agent2.setRole("AGENT");

        // Seed profiles with MANAGED entities
        AgentProfile p1 = new AgentProfile(); p1.setUser(agent); p1.setOnline(true); agentProfileRepository.save(p1);
        agent = userRepository.save(agent);
        agentId = agent.getId();

        AgentProfile p2 = new AgentProfile(); p2.setUser(agent2); p2.setOnline(true); agentProfileRepository.save(p2);
        agent2 = userRepository.save(agent2);
        agent2Id = agent2.getId();
    }

    @Test
    void testAcdRoutingAndEscalation() throws Exception {
        // 1. Create 4 Conversations concurrently-ish
        for(int i=0; i<4; i++) {
            CreateConversationRequest createReq = new CreateConversationRequest();
            createReq.setCustomerId(customerId);
            mockMvc.perform(post("/api/v1/conversations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isOk());
        }
        
        // Bob and Dave should each have 2 and 2... Wait, total 4 tickets. Capacity 3 per agent.
        // So they should be assigned.
        List<AgentProfile> profiles = agentProfileRepository.findAll();
        for (AgentProfile p : profiles) {
            assertEquals(2, p.getActiveConversationCount());
        }
        
        // Create 3 more tickets -> Total 7. 6 assigned, 1 unassigned in backlog.
        for(int i=0; i<3; i++) {
            CreateConversationRequest createReq = new CreateConversationRequest();
            createReq.setCustomerId(customerId);
            mockMvc.perform(post("/api/v1/conversations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)));
        }
        
        long unassignedCount = conversationRepository.findAll().stream().filter(c -> c.getStatus() == ConversationStatus.UNASSIGNED).count();
        assertEquals(1, unassignedCount);
        
        // Find a ticket assigned to Bob
        Conversation bobsTicket = conversationRepository.findAll().stream()
            .filter(c -> c.getAgent() != null && c.getAgent().getId().equals(agentId) && c.getStatus() == ConversationStatus.OPEN)
            .findFirst().get();

        // Bob escalates it
        EscalateRequest escalateReq = new EscalateRequest();
        escalateReq.setManagerId(managerId);

        mockMvc.perform(patch("/api/v1/conversations/" + bobsTicket.getId() + "/escalate")
                .header("X-Caller-Id", agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(escalateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"));
                
        // Wait a brief moment for the AFTER_COMMIT event listener to process the backlog
        // Polling for up to 2 seconds to allow the AFTER_COMMIT event to process
        int retries = 0;
        while (retries < 20) {
            unassignedCount = conversationRepository.findAll().stream().filter(c -> c.getStatus() == ConversationStatus.UNASSIGNED).count();
            if (unassignedCount == 0) break;
            Thread.sleep(100);
            retries++;
        }
        
        // The unassigned ticket should now be assigned to Bob because he freed up capacity!
        unassignedCount = conversationRepository.findAll().stream().filter(c -> c.getStatus() == ConversationStatus.UNASSIGNED).count();
        assertEquals(0, unassignedCount);

        AgentProfile bobProfile = agentProfileRepository.findById(agentId).get();
        assertEquals(3, bobProfile.getActiveConversationCount());
        assertEquals(1, bobProfile.getEscalatedConversationCount());
    }
}
