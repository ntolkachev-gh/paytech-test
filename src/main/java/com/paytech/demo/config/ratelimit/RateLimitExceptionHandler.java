package com.paytech.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class RateLimitExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RateLimitExceptionHandler.class);

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public String handleRateLimitException(RateLimitException ex, HttpServletRequest request, Model model) {
        log.warn("Rate limit exceeded for request: {}", request.getRequestURI());

        long waitTime = ex.getWaitTimeSeconds();
        String errorMessage = String.format(
                "Request limit exceeded. Please try again in %d seconds.",
                waitTime
        );

        model.addAttribute("error", errorMessage);
        model.addAttribute("retryAfter", waitTime);
        return "error";
    }
}

