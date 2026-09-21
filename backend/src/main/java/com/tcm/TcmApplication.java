package com.tcm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TcmApplication {
    public static void main(String[] args) {
        SpringApplication.run(TcmApplication.class, args);
    }
}
