package com.smartcampus.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IotServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotServiceApplication.class, args);
    }
}

