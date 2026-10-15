package com.pulsewatch.check;

import com.pulsewatch.ssrf.SsrfGuard;
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
    HealthCheck httpHealthCheck(SsrfGuard guard) {
        return new GuardedHealthCheck(new HttpHealthCheck(HttpHealthCheck.defaultClient()), guard);
    }

    @Bean
    HealthCheck tcpHealthCheck(SsrfGuard guard) {
        return new GuardedHealthCheck(new TcpHealthCheck(), guard);
    }

    @Bean(destroyMethod = "shutdownNow")
    ExecutorService checkExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}