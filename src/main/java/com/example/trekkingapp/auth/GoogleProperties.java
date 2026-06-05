package com.example.trekkingapp.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.google")
public class GoogleProperties {

    private String clientId;
    private List<String> clientIds = new ArrayList<>();
    private boolean mockEnabled = false;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getClientIds() {
        return clientIds;
    }

    public void setClientIds(List<String> clientIds) {
        this.clientIds = clientIds == null ? new ArrayList<>() : clientIds;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public Set<String> getResolvedClientIds() {
        Set<String> resolvedClientIds = new LinkedHashSet<>();
        addClientId(resolvedClientIds, clientId);
        for (String configuredClientId : clientIds) {
            addClientId(resolvedClientIds, configuredClientId);
        }
        return resolvedClientIds;
    }

    private void addClientId(Set<String> resolvedClientIds, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        for (String clientIdPart : value.split(",")) {
            String normalizedClientId = clientIdPart.trim();
            if (!normalizedClientId.isEmpty()) {
                resolvedClientIds.add(normalizedClientId);
            }
        }
    }
}
