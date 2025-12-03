package com.paytech.demo.paytech;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PaymentRequest(
    @JsonProperty("paymentType") String paymentType,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency
) {
}