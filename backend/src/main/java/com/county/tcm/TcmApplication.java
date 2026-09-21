package com.county.tcm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TcmApplication {
    public static void main(String[] args) {
        SpringApplication.run(TcmApplication.class, args);
    }
}
