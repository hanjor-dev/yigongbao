package com.yigongbao.module.imaging.v1.vo;

import lombok.Data;

import java.util.Map;

/**
 * 查看器初始化配置 VO（对应 temp.md 中的 kv 结构）
 *
 * @author hanjor
 * @date 2026-05-06
 */
@Data
public class ViewerConfigVO {

    private Paths paths;
    private Map<String, String> token;

    @Data
    public static class Paths {
        private PathItem dcmPath;
        private PathItem stlPath;
        private PathItem markPath;
    }

    @Data
    public static class PathItem {
        private String path;
        private Map<String, Object> params;
        private String type;
    }
}
