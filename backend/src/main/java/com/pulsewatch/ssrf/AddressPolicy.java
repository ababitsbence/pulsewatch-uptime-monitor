package com.pulsewatch.ssrf;

import java.net.Inet4Address;
import java.net.InetAddress;

public final class AddressPolicy {

    private AddressPolicy() {
    }

    public static boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        return address instanceof Inet4Address ? isBlockedIpv4(bytes) : isBlockedIpv6(bytes);
    }

    private static boolean isBlockedIpv4(byte[] b) {
        int first = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        int third = b[2] & 0xFF;
        return first == 0                                      // 0.0.0.0/8
                || (first == 100 && (second & 0xC0) == 64)     // 100.64.0.0/10 carrier-grade NAT
                || (first == 192 && second == 0 && third == 0) // 192.0.0.0/24
                || (first == 198 && (second & 0xFE) == 18)     // 198.18.0.0/15 benchmarking
                || first >= 240;                               // 240.0.0.0/4 reserved, incl. broadcast
    }

    private static boolean isBlockedIpv6(byte[] b) {
        if ((b[0] & 0xFE) == 0xFC) {
            return true; // fc00::/7 unique local
        }
        if (b[0] == 0x20 && b[1] == 0x02) {
            return true; // 2002::/16 6to4, embeds an IPv4 address
        }
        if (isNat64(b)) {
            return true; // 64:ff9b::/96, embeds an IPv4 address
        }
        for (int i = 0; i < 12; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return true; // ::/96 covers unspecified, loopback and IPv4-compatible addresses
    }

    private static boolean isNat64(byte[] b) {
        if (b[0] != 0x00 || b[1] != 0x64 || (b[2] & 0xFF) != 0xFF || (b[3] & 0xFF) != 0x9B) {
            return false;
        }
        for (int i = 4; i < 12; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return true;
    }
}