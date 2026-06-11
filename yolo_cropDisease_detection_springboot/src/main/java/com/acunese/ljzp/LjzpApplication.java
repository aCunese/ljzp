package com.acunese.ljzp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LjzpApplication {

    public static void main(String[] args) {
        SpringApplication.run(LjzpApplication.class, args);
    }

}
