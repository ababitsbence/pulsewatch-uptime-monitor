package com.pulsewatch.ssrf;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
@EnableConfigurationProperties(SsrfProperties.class)
public class SsrfConfig {

    @Bean
    HostResolver hostResolver() {
        return InetAddress::getAllByName;
    }
}