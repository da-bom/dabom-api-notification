package com.project.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NetworkValidator 내부 주소 검증 테스트")
class NetworkValidatorTest {

    @Nested
    @DisplayName("IPv4 주소")
    class IPv4 {

        @Test
        @DisplayName("loopback (127.0.0.1) → 내부 주소")
        void loopback() throws Exception {
            InetAddress addr = InetAddress.getByName("127.0.0.1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("link-local (169.254.1.1) → 내부 주소")
        void linkLocal() throws Exception {
            InetAddress addr = InetAddress.getByName("169.254.1.1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("site-local 10.x (10.0.0.1) → 내부 주소")
        void siteLocal10() throws Exception {
            InetAddress addr = InetAddress.getByName("10.0.0.1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("site-local 172.16.x (172.16.0.1) → 내부 주소")
        void siteLocal172() throws Exception {
            InetAddress addr = InetAddress.getByName("172.16.0.1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("site-local 192.168.x (192.168.1.1) → 내부 주소")
        void siteLocal192() throws Exception {
            InetAddress addr = InetAddress.getByName("192.168.1.1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("any-local (0.0.0.0) → 내부 주소")
        void anyLocal() throws Exception {
            InetAddress addr = InetAddress.getByName("0.0.0.0");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("공인 IP (8.8.8.8) → 외부 주소")
        void publicAddress() throws Exception {
            InetAddress addr = InetAddress.getByName("8.8.8.8");
            assertThat(NetworkValidator.isInternalAddress(addr)).isFalse();
        }
    }

    @Nested
    @DisplayName("IPv6 주소")
    class IPv6 {

        @Test
        @DisplayName("loopback (::1) → 내부 주소")
        void loopback() throws Exception {
            InetAddress addr = InetAddress.getByName("::1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("ULA (fd12:3456:789a::1) → 내부 주소")
        void uniqueLocalAddress() throws Exception {
            InetAddress addr = InetAddress.getByName("fd12:3456:789a::1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("공인 IPv6 (2001:4860:4860::8888) → 외부 주소")
        void publicAddress() throws Exception {
            InetAddress addr = InetAddress.getByName("2001:4860:4860::8888");
            assertThat(NetworkValidator.isInternalAddress(addr)).isFalse();
        }
    }

    @Nested
    @DisplayName("IPv4-mapped IPv6 주소")
    class IPv4MappedIPv6 {

        @Test
        @DisplayName("::ffff:127.0.0.1 → 내부 주소 (loopback)")
        void mappedLoopback() throws Exception {
            InetAddress addr = InetAddress.getByName("::ffff:127.0.0.1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("::ffff:192.168.1.1 → 내부 주소 (site-local)")
        void mappedSiteLocal() throws Exception {
            InetAddress addr = InetAddress.getByName("::ffff:192.168.1.1");
            assertThat(NetworkValidator.isInternalAddress(addr)).isTrue();
        }

        @Test
        @DisplayName("::ffff:8.8.8.8 → 외부 주소")
        void mappedPublic() throws Exception {
            InetAddress addr = InetAddress.getByName("::ffff:8.8.8.8");
            assertThat(NetworkValidator.isInternalAddress(addr)).isFalse();
        }
    }
}
