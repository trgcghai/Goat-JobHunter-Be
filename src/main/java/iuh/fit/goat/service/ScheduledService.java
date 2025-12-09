package iuh.fit.goat.service;

public interface ScheduledService {
    void handleDeactivateExpiredJobs();

    void handleSendSuitableJobs();

    void handleRefreshAiCache();
}
