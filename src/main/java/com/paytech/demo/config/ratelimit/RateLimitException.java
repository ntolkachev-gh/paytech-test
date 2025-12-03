package com.paytech.demo.config;

public class RateLimitException extends RuntimeException {
    
    private final long availableTokens;
    private final long waitTimeSeconds;

    public RateLimitException(String message, long availableTokens, long waitTimeSeconds) {
        super(message);
        this.availableTokens = availableTokens;
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public long getAvailableTokens() {
        return availableTokens;
    }

    public long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }
}

