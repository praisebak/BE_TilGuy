package com.tilguys.matilda.common.dlq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dlq")
public class DLQConfiguration {
    
    private int maxRetryCount = 3;
    private int retryDelayMinutes = 5;
    private int cleanupDays = 30;

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public int getRetryDelayMinutes() {
        return retryDelayMinutes;
    }

    public void setRetryDelayMinutes(int retryDelayMinutes) {
        this.retryDelayMinutes = retryDelayMinutes;
    }

    public int getCleanupDays() {
        return cleanupDays;
    }

    public void setCleanupDays(int cleanupDays) {
        this.cleanupDays = cleanupDays;
    }
}
