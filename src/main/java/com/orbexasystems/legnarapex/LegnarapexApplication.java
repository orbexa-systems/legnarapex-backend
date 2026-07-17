package com.orbexasystems.legnarapex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LegnarapexApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegnarapexApplication.class, args);
    }
}
