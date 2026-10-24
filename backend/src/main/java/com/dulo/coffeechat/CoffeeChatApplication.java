package com.dulo.coffeechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CoffeeChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeChatApplication.class, args);
    }
}
