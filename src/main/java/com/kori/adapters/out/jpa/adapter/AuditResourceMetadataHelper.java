package com.kori.adapters.out.jpa.adapter;

import java.util.HashMap;
import java.util.Map;

final class AuditResourceMetadataHelper {

    private static final String RESOURCE_TYPE = "resourceType";
    private static final String RESOURCE_REF = "resourceRef";

    private AuditResourceMetadataHelper() {
    }

    static Map<String, String> enrich(String action, Map<String, String> originalMetadata) {
        Map<String, String> metadata = new HashMap<>();
        if (originalMetadata != null) {
            metadata.putAll(originalMetadata);
        }

        String resourceType = blankToNull(metadata.get(RESOURCE_TYPE));
        String resourceRef = blankToNull(metadata.get(RESOURCE_REF));

        if (resourceType == null || resourceRef == null) {
            ResourceInfo inferred = inferResource(action, metadata);
            if (resourceType == null) {
                resourceType = inferred.type();
            }
            if (resourceRef == null) {
                resourceRef = inferred.ref();
            }
        }

        if (resourceType != null) {
            metadata.put(RESOURCE_TYPE, resourceType);
        }
        if (resourceRef != null) {
            metadata.put(RESOURCE_REF, resourceRef);
        }

        return metadata;
    }

    private static ResourceInfo inferResource(String action, Map<String, String> metadata) {
        if ("ADMIN_UPDATE_PLATFORM_CONFIG".equals(action)) {
            return new ResourceInfo("PLATFORM_CONFIG", "SYSTEM");
        }
        String merchantCode = blankToNull(metadata.get("merchantCode"));
        if (merchantCode != null) {
            return new ResourceInfo("MERCHANT", merchantCode);
        }
        String agentCode = blankToNull(metadata.get("agentCode"));
        if (agentCode != null) {
            return new ResourceInfo("AGENT", agentCode);
        }
        String clientCode = blankToNull(metadata.get("clientCode"));
        if (clientCode != null) {
            return new ResourceInfo("CLIENT", clientCode);
        }
        String terminalUid = blankToNull(metadata.get("terminalUid"));
        if (terminalUid != null) {
            return new ResourceInfo("TERMINAL", terminalUid);
        }
        return new ResourceInfo(null, null);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record ResourceInfo(String type, String ref) {
    }
}
