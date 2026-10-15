package com.pulsewatch.monitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TargetHostTest {

    @Test
    void extractsHostFromHttpUrl() {
        assertThat(TargetHost.of(MonitorType.HTTP, "http://example.com/path")).contains("example.com");
        assertThat(TargetHost.of(MonitorType.HTTP, "https://example.com:8443/x?y=1")).contains("example.com");
    }

    @Test
    void keepsBracketsAroundIpv6Literals() {
        assertThat(TargetHost.of(MonitorType.HTTP, "http://[::1]:8080/")).contains("[::1]");
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertThat(TargetHost.of(MonitorType.HTTP, "  http://example.com/  ")).contains("example.com");
    }

    @Test
    void returnsEmptyForMalformedHttpTargets() {
        assertThat(TargetHost.of(MonitorType.HTTP, "not a url")).isEmpty();
        assertThat(TargetHost.of(MonitorType.HTTP, "http:///path")).isEmpty();
    }

    @Test
    void extractsHostFromTcpTarget() {
        assertThat(TargetHost.of(MonitorType.TCP, "db.example.com:5432")).contains("db.example.com");
        assertThat(TargetHost.of(MonitorType.TCP, " 10.0.0.5:22 ")).contains("10.0.0.5");
    }

    @Test
    void returnsEmptyForMalformedTcpTargets() {
        assertThat(TargetHost.of(MonitorType.TCP, "nonsense")).isEmpty();
        assertThat(TargetHost.of(MonitorType.TCP, ":80")).isEmpty();
    }

    @Test
    void returnsEmptyForNullTarget() {
        assertThat(TargetHost.of(MonitorType.HTTP, null)).isEmpty();
    }
}