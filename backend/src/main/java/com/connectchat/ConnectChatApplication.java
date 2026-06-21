package com.connectchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ConnectChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectChatApplication.class, args);
    }
}
