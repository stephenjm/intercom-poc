MERGE INTO users (id, name, email, role) KEY(id)
    VALUES (1, 'Alice Agent', 'alice@support.com', 'AGENT');

MERGE INTO users (id, name, email, role) KEY(id)
    VALUES (2, 'Bob Customer', 'bob@example.com', 'CUSTOMER');

MERGE INTO users (id, name, email, role) KEY(id)
    VALUES (3, 'Charlie Manager', 'charlie@support.com', 'MANAGER');

MERGE INTO agent_profile (
                          user_id,
                          active_conversation_count,
                          escalated_conversation_count,
                          is_online,
                          last_assigned_at
    ) KEY(user_id)
    VALUES (1, 0, 0, true, CURRENT_TIMESTAMP);

MERGE INTO agent_profile (
                          user_id,
                          active_conversation_count,
                          escalated_conversation_count,
                          is_online,
                          last_assigned_at
    ) KEY(user_id)
    VALUES (3, 0, 0, true, CURRENT_TIMESTAMP);

--INSERT INTO users (id, name, email, role) VALUES (1, 'Alice Agent', 'alice@support' ||.com', 'AGENT');
--INSERT INTO users (id, name, email, role) VALUES (2, 'Bob Customer', 'bob@example' ||'.com', 'CUSTOMER');
--INSERT INTO users (id, name, email, role) VALUES (3, 'Charlie Manager','charlie@support.com', 'MANAGER');

--CREATE INDEX idx_agent_routing ON agent_profile (is_online, active_conversation_count,last_assigned_at);
--INSERT INTO agent_profile (user_id,active_conversation_count,escalated_conversation_count,is_online,last_assigned_at) VALUES (1,0,0,true,CURRENT_TIMESTAMP);
--INSERT INTO agent_profile (user_id,active_conversation_count,escalated_conversation_count,is_online,last_assigned_at) VALUES (3,0,0,true,CURRENT_TIMESTAMP);