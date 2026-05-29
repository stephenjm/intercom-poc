#!/bin/bash
echo "Generating traffic for Grafana Dashboard... Press Ctrl+C to stop."

# URLs
HEALTH_URL="http://localhost:8180/actuator/health"
CREATE_URL="http://localhost:8180/api/v1/conversations"

while true; do
  # 1. Ping Health
  curl -s -o /dev/null $HEALTH_URL

  # 2. Create a conversation (will fail occasionally if ID 1 isn't there, but generates metrics regardless)
  curl -s -X POST -H "Content-Type: application/json" -d '{"customerId": 1}' $CREATE_URL > /dev/null

  # 3. Simulate an error (invalid assign)
  curl -s -X PATCH -H "Content-Type: application/json" -H "X-Caller-Id: 2" -d '{"agentId": 4}' http://localhost:8180/api/v1/conversations/1/assign > /dev/null

  echo -n "."
  sleep 0.5
done
