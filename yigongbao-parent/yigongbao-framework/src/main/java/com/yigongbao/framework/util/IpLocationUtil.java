package com.yigongbao.framework.util;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

/**
 * IP 归属地解析工具（基于 ip2region 离线库）
 * xdb 文件需放在 classpath 根目录：resources/ip2region.xdb
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Slf4j
public class IpLocationUtil {

    private static final Searcher SEARCHER;

    static {
        Searcher s = null;
        try {
            InputStream inputStream = new ClassPathResource("ip2region_v4.xdb").getInputStream();
            byte[] dbBytes = inputStream.readAllBytes();
            // 全量加载到内存，查询无 IO，线程安全
            s = Searcher.newWithBuffer(dbBytes);
            log.info("ip2region 初始化成功");
        } catch (Exception e) {
            log.error("ip2region 初始化失败，IP归属地解析功能不可用", e);
        }
        SEARCHER = s;
    }

    private IpLocationUtil() {
    }

    /**
     * 解析 IP 归属地
     * <p>
     * 内网 IP 直接返回"内网IP"；解析失败返回"未知"
     *
     * @param ip IPv4 地址
     * @return 归属地，如"中国 广东省 深圳市 电信"
     */
    public static String getLocation(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return "未知";
        }
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        if (SEARCHER == null) {
            return "未知";
        }
        try {
            String region = SEARCHER.search(ip);
            return formatRegion(region);
        } catch (Exception e) {
            log.debug("IP归属地解析失败，ip={}", ip, e);
            return "未知";
        }
    }

    /**
     * 判断是否为内网 IP
     */
    private static boolean isInternalIp(String ip) {
        return "127.0.0.1".equals(ip)
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || (ip.startsWith("172.") && isInRange172(ip));
    }

    private static boolean isInRange172(String ip) {
        try {
            int second = Integer.parseInt(ip.split("\\.")[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将 "中国|0|广东省|深圳市|电信" 格式化为 "中国 广东省 深圳市 电信"，过滤占位符 "0"
     */
    private static String formatRegion(String region) {
        if (region == null || region.isBlank()) {
            return "未知";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : region.split("\\|")) {
            if (!"0".equals(part) && !part.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(part);
            }
        }
        return sb.isEmpty() ? "未知" : sb.toString();
    }
}
