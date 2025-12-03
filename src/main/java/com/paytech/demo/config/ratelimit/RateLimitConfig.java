package com.paytech.demo.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    @Value("${rate-limit.requests-per-minute:10}")
    private int requestsPerMinute;

    @Value("${rate-limit.requests-per-hour:100}")
    private int requestsPerHour;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> {
            Bandwidth limitPerMinute = Bandwidth.classic(
                    requestsPerMinute,
                    Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
            );

            Bandwidth limitPerHour = Bandwidth.classic(
                    requestsPerHour,
                    Refill.intervally(requestsPerHour, Duration.ofHours(1))
            );

            return Bucket.builder()
                    .addLimit(limitPerMinute)
                    .addLimit(limitPerHour)
                    .build();
        });
    }

    public void resetBucket(String key) {
        cache.remove(key);
    }

    public void resetAllBuckets() {
        cache.clear();
    }
}

