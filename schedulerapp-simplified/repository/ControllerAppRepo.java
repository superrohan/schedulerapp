package com.ubs.bigid.schedulerapp.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for calling ControllerApp endpoints with OAuth2 security
 * OAuth2 token is automatically injected by OAuth2BearerTokenInterceptor
 */
@Repository
public class ControllerAppRepo {

    private static final Logger logger = LoggerFactory.getLogger(ControllerAppRepo.class);
    
    private final RestTemplate controllerAppRestTemplate;

    public ControllerAppRepo(@Qualifier("controllerAppRestTemplate") RestTemplate controllerAppRestTemplate) {
        this.controllerAppRestTemplate = controllerAppRestTemplate;
    }

    /**
     * Get recently completed scans
     */
    public String getRecentlyCompletedScans(String fromDate, String toDate, 
                                           int completedWithinDays, int scanLimit) {
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
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error calling ControllerApp: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get active scan cycle by ID
     */
    public String getActiveScanCycleById(long scanCycleId) {
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scan-cycles/active/" + scanCycleId,
                    String.class
            );

            logger.debug("Response Returned -> {}", response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error calling ControllerApp: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get scan cycle by ID
     */
    public String getScanCycleById(long scanCycleId) {
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scan-cycles/" + scanCycleId,
                    String.class
            );

            logger.debug("Response Returned -> {}", response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error calling ControllerApp: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get scan cycle by data target name
     */
    public String getScanCycleByDataTargetName(String name) {
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scan-cycles/data-target/" + name,
                    String.class
            );

            logger.debug("Response Returned -> {}", response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error calling ControllerApp: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get scan cycles by status
     */
    public String getScansByStatus(String status) {
        try {
            Map<String, String> queryStrings = new HashMap<>();
            queryStrings.put("status", status);

            ResponseEntity<String> response = controllerAppRestTemplate.getForEntity(
                    "/controller/internal/scans/status?status={status}",
                    String.class,
                    queryStrings
            );

            logger.debug("Response Returned -> {}", response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error calling ControllerApp: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Launch a scan cycle (POST request)
     */
    public String launchScanCycle(long scanCycleId) {
        try {
            ResponseEntity<String> response = controllerAppRestTemplate.postForEntity(
                    "/controller/internal/scheduler/launch-scan/" + scanCycleId,
                    null,
                    String.class
            );

            logger.info("Scan cycle {} launched successfully. Response: {}", scanCycleId, response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error launching scan cycle {}: {}", scanCycleId, e.getMessage());
            throw e;
        }
    }
}
