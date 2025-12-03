package com.paytech.demo.paytech;

import com.paytech.demo.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "paytechClient",
    url = "${paytech.base-url}",
    configuration = FeignConfig.class
)
public interface PaytechClient {
    @PostMapping("/api/v1/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request);
}

