package com.docket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocketApplication.class, args);
    }
}
