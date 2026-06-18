package com.giftgpt.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.giftgpt")
public class GiftGptApplication {

    public static void main(String[] args) {
        SpringApplication.run(GiftGptApplication.class, args);
    }
}
