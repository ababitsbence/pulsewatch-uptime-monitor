package com.pulsewatch.ssrf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class AddressPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", "127.1.2.3",
            "10.0.0.1", "10.255.255.255",
            "172.16.0.1", "172.31.255.255",
            "192.168.1.1",
            "169.254.169.254",
            "0.0.0.0", "0.1.2.3",
            "100.64.0.1", "100.127.255.255",
            "198.18.0.1", "198.19.255.255",
            "240.0.0.1", "255.255.255.255",
            "224.0.0.1",
            "::1", "::", "fe80::1", "fc00::1", "fd12:3456:789a::1",
            "::ffff:127.0.0.1", "::ffff:10.0.0.1",
            "::7f00:1", "64:ff9b::7f00:1", "2002:7f00:1::1"
    })
    void blocksPrivateAndReservedAddresses(String literal) throws UnknownHostException {
        assertThat(AddressPolicy.isBlocked(InetAddress.getByName(literal))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "8.8.8.8", "1.1.1.1", "93.184.216.34",
            "172.15.255.255", "172.32.0.1",
            "100.63.255.255", "100.128.0.1",
            "198.17.255.255", "198.20.0.1",
            "::ffff:8.8.8.8",
            "2606:4700:4700::1111", "2001:4860:4860::8888"
    })
    void allowsPublicAddresses(String literal) throws UnknownHostException {
        assertThat(AddressPolicy.isBlocked(InetAddress.getByName(literal))).isFalse();
    }
}