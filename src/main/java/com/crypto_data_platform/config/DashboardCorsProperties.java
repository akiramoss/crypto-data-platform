package com.crypto_data_platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dashboard.cors")
public class DashboardCorsProperties {

    private String allowedOrigin = "http://localhost:5173";
}
