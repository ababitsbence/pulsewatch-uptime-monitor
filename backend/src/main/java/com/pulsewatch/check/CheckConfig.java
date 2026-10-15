package com.pulsewatch.check;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CheckProperties.class)
public class CheckConfig {

    @Bean
    HttpHealthCheck httpHealthCheck() {
        return new HttpHealthCheck(HttpHealthCheck.defaultClient());
    }

    @Bean
    TcpHealthCheck tcpHealthCheck() {
        return new TcpHealthCheck();
    }

    @Bean(destroyMethod = "shutdownNow")
    ExecutorService checkExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}