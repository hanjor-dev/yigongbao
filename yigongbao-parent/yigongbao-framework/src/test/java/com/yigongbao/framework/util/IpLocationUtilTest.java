package com.yigongbao.framework.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IpLocationUtil 单元测试
 *
 * @author hanjor
 * @date 2026-04-10
 */
class IpLocationUtilTest {

    @Test
    void testLocalhost_returnsInternalIp() {
        assertEquals("内网IP", IpLocationUtil.getLocation("127.0.0.1"));
    }

    @Test
    void testPrivateIp_192_returnsInternalIp() {
        assertEquals("内网IP", IpLocationUtil.getLocation("192.168.1.100"));
    }

    @Test
    void testPrivateIp_10_returnsInternalIp() {
        assertEquals("内网IP", IpLocationUtil.getLocation("10.0.0.1"));
    }

    @Test
    void testPrivateIp_172_returnsInternalIp() {
        assertEquals("内网IP", IpLocationUtil.getLocation("172.16.0.1"));
    }

    @Test
    void testNull_returnsUnknown() {
        assertEquals("未知", IpLocationUtil.getLocation(null));
    }

    @Test
    void testBlank_returnsUnknown() {
        assertEquals("未知", IpLocationUtil.getLocation("   "));
    }

    @Test
    void testUnknown_returnsUnknown() {
        assertEquals("未知", IpLocationUtil.getLocation("unknown"));
    }

    @Test
    void testPublicIp_chinaUnicom() {
        // 联通公网 IP
        String location = IpLocationUtil.getLocation("112.80.248.75");
        System.out.println("112.80.248.75 -> " + location);
        assertNotNull(location);
        assertNotEquals("未知", location);
        assertTrue(location.contains("中国"));
    }

    @Test
    void testPublicIp_chinaTelecom() {
        // 电信公网 IP
        String location = IpLocationUtil.getLocation("61.135.169.121");
        System.out.println("61.135.169.121 -> " + location);
        assertNotNull(location);
        assertNotEquals("未知", location);
        assertTrue(location.contains("中国"));
    }

    @Test
    void testPublicIp_overseas() {
        // Google DNS，美国
        String location = IpLocationUtil.getLocation("8.8.8.8");
        System.out.println("8.8.8.8 -> " + location);
        assertNotNull(location);
        assertNotEquals("未知", location);
    }
}
