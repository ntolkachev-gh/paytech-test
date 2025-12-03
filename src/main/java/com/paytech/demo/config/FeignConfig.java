package com.paytech.demo.config;

import feign.RequestInterceptor;
import feign.Retryer;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class FeignConfig {
    private static final Logger log = LoggerFactory.getLogger(FeignConfig.class);
    
    @Value("${paytech.api-token}")
    private String apiToken;
    
    @Value("${feign.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${feign.retry.period:1000}")
    private long period;
    
    @Value("${feign.retry.max-period:5000}")
    private long maxPeriod;
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Authorization", "Bearer " + apiToken);
            requestTemplate.header("Content-Type", "application/json");
        };
    }

    @Bean
    public Retryer feignRetryer() {
        log.info("Configuring Feign Retryer: maxAttempts={}, period={}ms, maxPeriod={}ms", 
                maxAttempts, period, maxPeriod);
        
        return new Retryer() {
            private int attempt = 1;
            private long currentInterval = period;

            @Override
            public void continueOrPropagate(RetryableException e) {
                if (attempt++ >= maxAttempts) {
                    log.error("Feign retry exhausted after {} attempts. Last error: {}", 
                            maxAttempts - 1, e.getMessage());
                    throw e;
                }
                
                try {
                    log.warn("Feign retry attempt {}/{} after {}ms delay. Error: {}", 
                            attempt - 1, maxAttempts, currentInterval, e.getMessage());
                    Thread.sleep(currentInterval);

                    currentInterval = Math.min((long) (currentInterval * 1.5), maxPeriod);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }

            @Override
            public Retryer clone() {
                return new Retryer.Default(period, maxPeriod, maxAttempts);
            }
        };
    }
}

