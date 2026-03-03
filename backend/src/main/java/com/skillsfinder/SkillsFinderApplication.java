package com.skillsfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkillsFinderApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkillsFinderApplication.class, args);
    }
}
