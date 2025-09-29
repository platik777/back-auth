package ru.platik777.backauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@ConfigurationPropertiesScan
public class BackAuthApplication {

    public static void main(String[] args) {
        System.out.println("Starting Back-Auth...");
        SpringApplication.run(BackAuthApplication.class, args);
    }
}