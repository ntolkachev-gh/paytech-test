package com.paytech.demo.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIpAddress(request);
        var bucket = rateLimitConfig.resolveBucket(clientIp);

        if (!bucket.tryConsume(1)) {
            var probe = bucket.tryConsumeAndReturnRemaining(0);
            long availableTokens = probe.getRemainingTokens();

            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            
            log.warn("Rate limit exceeded for IP: {}. Available tokens: {}, Wait time: {} seconds", 
                    clientIp, availableTokens, waitTimeSeconds);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(waitTimeSeconds));
            response.setHeader(RATE_LIMIT_HEADER, String.valueOf(availableTokens));
            response.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(waitTimeSeconds));

            String acceptHeader = request.getHeader("Accept");
            boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
            
            if (isJsonRequest) {
                response.setContentType("application/json");
                String errorMessage = String.format(
                        "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again in %d seconds.\",\"retryAfter\":%d}",
                        waitTimeSeconds, waitTimeSeconds
                );
                response.getWriter().write(errorMessage);
            } else {
                request.getSession().setAttribute("rateLimitError", 
                        String.format("Request limit exceeded. Please try again in %d seconds.", waitTimeSeconds));
                request.getSession().setAttribute("retryAfter", waitTimeSeconds);
                response.sendRedirect("/error?rateLimit=true");
            }
            return;
        }

        var probe = bucket.tryConsumeAndReturnRemaining(0);
        response.setHeader(RATE_LIMIT_HEADER, String.valueOf(probe.getRemainingTokens()));
        
        filterChain.doFilter(request, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/error");
    }
}

