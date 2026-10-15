package com.pulsewatch.check;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CheckConfig {

    @Bean
    HttpHealthCheck httpHealthCheck() {
        return new HttpHealthCheck(HttpHealthCheck.defaultClient());
    }

    @Bean
    TcpHealthCheck tcpHealthCheck() {
        return new TcpHealthCheck();
    }
}