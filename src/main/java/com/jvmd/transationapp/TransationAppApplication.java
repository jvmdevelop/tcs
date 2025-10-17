package com.jvmd.transationapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.sql.Connection;

@SpringBootApplication
public class TransationAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransationAppApplication.class, args);
    }

}
