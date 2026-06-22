package com.laioffer.dispatchdeliveryapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DispatchDeliveryAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchDeliveryAppApplication.class, args);
    }

}
