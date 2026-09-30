package com.telecom.activation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MobileActivationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobileActivationApplication.class, args);
    }
}
