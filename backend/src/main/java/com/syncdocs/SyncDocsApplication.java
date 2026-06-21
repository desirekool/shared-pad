package com.syncdocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class SyncDocsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncDocsApplication.class, args);
    }
}
