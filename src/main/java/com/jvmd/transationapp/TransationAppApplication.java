package com.jvmd.transationapp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@Slf4j
public class TransationAppApplication {
    public static void main(String[] args) {
        log.info("Starting Fraud Detection Application...");
        SpringApplication.run(TransationAppApplication.class, args);
        log.info("Fraud Detection Application started successfully!");
    }
}
