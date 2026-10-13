package com.opsmind.agent.application;

import java.util.UUID;

public record AgentToolInput(String query, UUID documentId) {
}
