package com.example.activitybookingsystem.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class MinioConfig {

    private final MinioProperties miniProperties;

    public MinioConfig(MinioProperties miniProperties) {
        this.miniProperties = miniProperties;
    }

    @Bean // 方法返回的对象被注册为Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(miniProperties.getEndpoint())
                .credentials(miniProperties.getAccessKey(), miniProperties.getSecretKey())
                .build();
    }
}
