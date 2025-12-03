package com.paytech.demo.paytech;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaytechService {
    private static final Logger log = LoggerFactory.getLogger(PaytechService.class);

    private final PaytechClient paytechClient;

    public PaytechService(PaytechClient paytechClient) {
        this.paytechClient = paytechClient;
    }

    @Retryable(
            retryFor = {FeignException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5, maxDelay = 5000)
    )
    public String createDeposit(BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest("DEPOSIT", amount, "EUR");
            log.debug("Sending payment creation request: amount={}, currency=EUR", amount);

            PaymentResponse response = paytechClient.createPayment(request);
            log.debug("Received response from PayTech API");

            String redirectUrl = response.getRedirectUrl();
            if (redirectUrl == null || redirectUrl.isEmpty()) {
                log.error("redirectUrl is missing in API response");
                throw new RuntimeException("redirectUrl not found in API response");
            }

            log.info("Payment successfully created, redirectUrl received");
            return redirectUrl;

        } catch (FeignException e) {
            int status = e.status();

            if (status >= 400 && status < 500) {
                log.error("Client error when calling PayTech API: status={}, content={}",
                        status, e.contentUTF8());
                throw new RuntimeException(
                        "Invalid request to PayTech API: " + status + " - " + e.contentUTF8(), e);
            }

            log.warn("Server error when calling PayTech API: status={}. Retry will be attempted.",
                    status);
            throw e;
            
        } catch (Exception e) {
            log.error("Unexpected error creating deposit", e);
            throw new RuntimeException("Error creating deposit: " + e.getMessage(), e);
        }
    }
}

