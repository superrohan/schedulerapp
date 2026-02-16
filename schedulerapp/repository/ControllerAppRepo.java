package com.ubs.bigid.schedulerapp.repository;

import com.ubs.bigid.schedulerapp.audit.ServiceAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for calling ControllerApp endpoints with OAuth2 security
 * OAuth2 token is automatically injected by OAuth2RestTemplateInterceptor
 */
@Repository
public class ControllerAppRepo {

    private static final Logger logger = LoggerFactory.getLogger(ControllerAppRepo.class);
    
    private final RestTemplate controllerAppRestTemplate;
    private final ServiceAuditService auditService;

    public ControllerAppRepo(
            @Qualifier("controllerAppRestTemplate") RestTemplate controllerAppRestTemplate,
            ServiceAuditService auditService) {
        this.controllerAppRestTemplate = controllerAppRestTemplate;
        this.auditService = auditService;
    }

    /**
     * Get recently completed scans
     */
    public String getRecentlyCompletedScans(String fromDate, String toDate, 
                                           int completedWithinDays, int scanLimit) {
        String correlationId = ensureCorrelationId();
        auditService.logStart("GET_RECENTLY_COMPLETED_SCANS", "N/A");
        
        try {
            Map<String, String> queryStrings = new HashMap<>();
            queryStrings.put("withinDays", String.valueOf(completedWithinDays));
            queryStrings.put("scanLimit", String.valueOf(scanLimit));
            queryStrings.put("fromDate", fromDate);
            queryStrings.put("toDate", toDate);

            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scans/recently-completed?withinDays={withinDays}&scanLimit={scanLimit}&fromDate={fromDate}&toDate={toDate}",
                    String.class,
                    queryStrings
            );

            logger.debug("Response Returned -> {}", response.getBody());
            auditService.logSuccess("GET_RECENTLY_COMPLETED_SCANS", "N/A");
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling ControllerApp: {} - {}", e.getStatusCode(), e.getMessage());
            auditService.logFailure("GET_RECENTLY_COMPLETED_SCANS", "N/A", e.getMessage());
            throw e;
        }
    }

    /**
     * Get active scan cycle by ID
     */
    public String getActiveScanCycleById(long scanCycleId) {
        String correlationId = ensureCorrelationId();
        String scanCycleIdStr = String.valueOf(scanCycleId);
        auditService.logStart("GET_ACTIVE_SCAN_CYCLE", scanCycleIdStr);
        
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scan-cycles/active/" + scanCycleId,
                    String.class
            );

            logger.debug("Response Returned -> {}", response.getBody());
            auditService.logSuccess("GET_ACTIVE_SCAN_CYCLE", scanCycleIdStr);
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling ControllerApp: {} - {}", e.getStatusCode(), e.getMessage());
            auditService.logFailure("GET_ACTIVE_SCAN_CYCLE", scanCycleIdStr, e.getMessage());
            throw e;
        }
    }

    /**
     * Get scan cycle by ID
     */
    public String getScanCycleById(long scanCycleId) {
        String correlationId = ensureCorrelationId();
        String scanCycleIdStr = String.valueOf(scanCycleId);
        auditService.logStart("GET_SCAN_CYCLE", scanCycleIdStr);
        
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scan-cycles/" + scanCycleId,
                    String.class
            );

            logger.debug("Response Returned -> {}", response.getBody());
            auditService.logSuccess("GET_SCAN_CYCLE", scanCycleIdStr);
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling ControllerApp: {} - {}", e.getStatusCode(), e.getMessage());
            auditService.logFailure("GET_SCAN_CYCLE", scanCycleIdStr, e.getMessage());
            throw e;
        }
    }

    /**
     * Get scan cycle by data target name
     */
    public String getScanCycleByDataTargetName(String name) {
        String correlationId = ensureCorrelationId();
        auditService.logStart("GET_SCAN_CYCLE_BY_NAME", name);
        
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scan-cycles/data-target/" + name,
                    String.class
            );

            logger.debug("Response Returned -> {}", response.getBody());
            auditService.logSuccess("GET_SCAN_CYCLE_BY_NAME", name);
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling ControllerApp: {} - {}", e.getStatusCode(), e.getMessage());
            auditService.logFailure("GET_SCAN_CYCLE_BY_NAME", name, e.getMessage());
            throw e;
        }
    }

    /**
     * Get scan cycles by status
     */
    public String getScansByStatus(String status) {
        String correlationId = ensureCorrelationId();
        auditService.logStart("GET_SCANS_BY_STATUS", status);
        
        try {
            Map<String, String> queryStrings = new HashMap<>();
            queryStrings.put("status", status);

            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scans/status?status={status}",
                    String.class,
                    queryStrings
            );

            logger.debug("Response Returned -> {}", response.getBody());
            auditService.logSuccess("GET_SCANS_BY_STATUS", status);
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling ControllerApp: {} - {}", e.getStatusCode(), e.getMessage());
            auditService.logFailure("GET_SCANS_BY_STATUS", status, e.getMessage());
            throw e;
        }
    }

    /**
     * Launch a scan cycle (POST request)
     */
    public String launchScanCycle(long scanCycleId) {
        String correlationId = ensureCorrelationId();
        String scanCycleIdStr = String.valueOf(scanCycleId);
        auditService.logStart("LAUNCH_SCAN", scanCycleIdStr);
        
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.postForEntity(
                    "/controller/internal/scheduler/launch-scan/" + scanCycleId,
                    null,
                    String.class
            );

            logger.info("Scan cycle {} launched successfully. Response: {}", scanCycleId, response.getBody());
            auditService.logSuccess("LAUNCH_SCAN", scanCycleIdStr, "Scan launched via scheduler");
            
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error launching scan cycle {}: {} - {}", scanCycleId, e.getStatusCode(), e.getMessage());
            auditService.logFailure("LAUNCH_SCAN", scanCycleIdStr, e.getMessage());
            throw e;
        }
    }

    /**
     * Ensures correlation ID exists in MDC, creates one if not present
     */
    private String ensureCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
            logger.debug("Generated correlation ID for service call: {}", correlationId);
        }
        return correlationId;
    }
}
