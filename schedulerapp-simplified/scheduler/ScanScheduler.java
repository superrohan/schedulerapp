package com.ubs.bigid.schedulerapp.scheduler;

import com.ubs.bigid.schedulerapp.repository.ControllerAppRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Example scheduler that demonstrates secure service-to-service communication
 * with ControllerApp using OAuth2 Client Credentials flow
 */
@Component
public class ScanScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ScanScheduler.class);
    
    private final ControllerAppRepo controllerAppRepo;

    public ScanScheduler(ControllerAppRepo controllerAppRepo) {
        this.controllerAppRepo = controllerAppRepo;
    }

    /**
     * Scheduled job that runs every hour to check and launch scans
     * Cron: Every hour at minute 0
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledScanLauncher() {
        try {
            logger.info("=== Scheduled Scan Launcher Started ===");
            
            // Example: Get scans by status
            String pendingScans = controllerAppRepo.getScansByStatus("PENDING");
            logger.info("Retrieved pending scans: {}", pendingScans);
            
            // Example: Launch a specific scan cycle
            // In real implementation, parse pendingScans and launch appropriate cycles
            // For demo purposes, using a hardcoded scan cycle ID
            // long scanCycleId = extractScanCycleId(pendingScans);
            // String response = controllerAppRepo.launchScanCycle(scanCycleId);
            
            logger.info("=== Scheduled Scan Launcher Completed ===");
            
        } catch (Exception e) {
            logger.error("Error in scheduled scan launcher", e);
        }
    }

    /**
     * Example: Daily scan cycle launcher
     * Cron: Every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyScanCycleLauncher() {
        try {
            logger.info("=== Daily Scan Cycle Launcher Started ===");
            
            // Example: Get recently completed scans from last 7 days
            String recentScans = controllerAppRepo.getRecentlyCompletedScans(
                    "2026-02-09", 
                    "2026-02-16", 
                    7, 
                    50
            );
            
            logger.info("Retrieved recently completed scans: {}", recentScans);
            
            logger.info("=== Daily Scan Cycle Launcher Completed ===");
            
        } catch (Exception e) {
            logger.error("Error in daily scan cycle launcher", e);
        }
    }

    /**
     * Manual trigger for testing - launches a specific scan cycle
     */
    public void launchScanManually(long scanCycleId) {
        try {
            logger.info("=== Manual Scan Launch - Scan Cycle ID: {} ===", scanCycleId);
            
            String response = controllerAppRepo.launchScanCycle(scanCycleId);
            logger.info("Scan launched successfully: {}", response);
            
        } catch (Exception e) {
            logger.error("Error launching scan manually", e);
        }
    }

    /**
     * Example: Monitor active scan cycles
     * Cron: Every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void monitorActiveScanCycles() {
        try {
            logger.debug("=== Monitoring Active Scan Cycles ===");
            
            // Get active scans
            String activeScans = controllerAppRepo.getScansByStatus("ACTIVE");
            logger.debug("Active scans: {}", activeScans);
            
            // In real implementation, parse and process active scans
            
        } catch (Exception e) {
            logger.error("Error monitoring active scan cycles", e);
        }
    }
}
