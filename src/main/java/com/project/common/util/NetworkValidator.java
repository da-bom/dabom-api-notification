package com.project.common.util;

import java.net.Inet6Address;
import java.net.InetAddress;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NetworkValidator {

    private static final byte FC00_MASK = (byte) 0xfe;
    private static final byte FC00_PREFIX = (byte) 0xfc;

    public static boolean isInternalAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress()) {
            return true;
        }

        if (addr instanceof Inet6Address) {
            byte[] bytes = addr.getAddress();

            if (isIPv4MappedIPv6(bytes)) {
                byte[] ipv4Bytes = new byte[4];
                System.arraycopy(bytes, 12, ipv4Bytes, 0, 4);
                try {
                    return isInternalAddress(InetAddress.getByAddress(ipv4Bytes));
                } catch (java.net.UnknownHostException e) {
                    return true;
                }
            }

            if ((bytes[0] & FC00_MASK) == FC00_PREFIX) {
                return true;
            }
        }

        return false;
    }

    private static boolean isIPv4MappedIPv6(byte[] bytes) {
        if (bytes.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }
}
