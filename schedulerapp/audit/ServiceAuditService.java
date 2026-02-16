package com.ubs.bigid.schedulerapp.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for auditing service-to-service operations
 */
@Service
public class ServiceAuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("SERVICE_AUDIT");
    private static final String SERVICE_NAME = "schedulerapp-service";
    private final ObjectMapper objectMapper;

    public ServiceAuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Log a service action with full context
     */
    public void logServiceAction(String action, String scanId, String status, String details) {
        Map<String, Object> auditEntry = new HashMap<>();
        auditEntry.put("service", SERVICE_NAME);
        auditEntry.put("action", action);
        auditEntry.put("scanId", scanId);
        auditEntry.put("status", status);
        auditEntry.put("timestamp", Instant.now().toString());
        auditEntry.put("correlationId", MDC.get("correlationId"));
        
        if (details != null) {
            auditEntry.put("details", details);
        }

        try {
            String auditJson = objectMapper.writeValueAsString(auditEntry);
            auditLogger.info(auditJson);
        } catch (JsonProcessingException e) {
            auditLogger.error("Failed to serialize audit entry", e);
        }
    }

    /**
     * Log successful action
     */
    public void logSuccess(String action, String scanId) {
        logServiceAction(action, scanId, "SUCCESS", null);
    }

    /**
     * Log successful action with details
     */
    public void logSuccess(String action, String scanId, String details) {
        logServiceAction(action, scanId, "SUCCESS", details);
    }

    /**
     * Log failed action
     */
    public void logFailure(String action, String scanId, String errorMessage) {
        logServiceAction(action, scanId, "FAILURE", errorMessage);
    }

    /**
     * Log action start
     */
    public void logStart(String action, String scanId) {
        logServiceAction(action, scanId, "STARTED", null);
    }
}
