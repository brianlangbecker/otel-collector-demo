package com.example.statsd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StatsdDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatsdDemoApplication.class, args);
    }
}
