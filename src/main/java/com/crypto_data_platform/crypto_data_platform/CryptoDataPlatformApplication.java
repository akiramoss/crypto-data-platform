package com.crypto_data_platform.crypto_data_platform;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@EnableScheduling // Ejecutar tareas automáticas
@SpringBootApplication
public class CryptoDataPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoDataPlatformApplication.class, args);
    }
}
