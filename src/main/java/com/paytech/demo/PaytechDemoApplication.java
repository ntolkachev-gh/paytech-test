package com.paytech.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.paytech.demo.paytech")
public class PaytechDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaytechDemoApplication.class, args);
    }
}

