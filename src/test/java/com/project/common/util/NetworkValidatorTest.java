package com.project.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("NetworkValidator 내부 주소 검증 테스트")
class NetworkValidatorTest {

    @ParameterizedTest(name = "{0} → internal={1}")
    @MethodSource("addressProvider")
    @DisplayName("isInternalAddress 검증")
    void isInternalAddress(String address, boolean expected) throws Exception {
        InetAddress addr = InetAddress.getByName(address);
        assertThat(NetworkValidator.isInternalAddress(addr)).isEqualTo(expected);
    }

    static Stream<Arguments> addressProvider() {
        return Stream.of(
                // IPv4 내부 주소
                Arguments.of("127.0.0.1", true),
                Arguments.of("169.254.1.1", true),
                Arguments.of("10.0.0.1", true),
                Arguments.of("172.16.0.1", true),
                Arguments.of("192.168.1.1", true),
                Arguments.of("0.0.0.0", true),
                // IPv4 외부 주소
                Arguments.of("8.8.8.8", false),
                // IPv6 내부 주소
                Arguments.of("::1", true),
                Arguments.of("fd12:3456:789a::1", true),
                // IPv6 외부 주소
                Arguments.of("2001:4860:4860::8888", false),
                // IPv4-mapped IPv6
                Arguments.of("::ffff:127.0.0.1", true),
                Arguments.of("::ffff:192.168.1.1", true),
                Arguments.of("::ffff:8.8.8.8", false));
    }
}
