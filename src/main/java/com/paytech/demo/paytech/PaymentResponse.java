package com.paytech.demo.paytech;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentResponse(
    @JsonProperty("redirectUrl") String redirectUrl,
    @JsonProperty("result") Result result
) {
    public String getRedirectUrl() {
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            return redirectUrl;
        }
        if (result != null && result.redirectUrl() != null) {
            return result.redirectUrl();
        }
        return null;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
        @JsonProperty("redirectUrl") String redirectUrl
    ) {
    }
}

