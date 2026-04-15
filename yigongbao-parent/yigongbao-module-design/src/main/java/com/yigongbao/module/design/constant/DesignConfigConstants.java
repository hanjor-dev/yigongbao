package com.yigongbao.module.design.constant;

/**
 * 设计模块配置常量
 *
 * @author hanjor
 * @date 2026-04-15
 */
public final class DesignConfigConstants {

    private DesignConfigConstants() {
    }

    /**
     * 数据包允许的文件扩展名（逗号分隔）
     * 默认值：.stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp
     */
    public static final String PACKAGE_ALLOWED_EXTENSIONS = "design.package.allowed_extensions";

    /**
     * 数据包允许的文件扩展名默认值
     */
    public static final String PACKAGE_ALLOWED_EXTENSIONS_DEFAULT = ".stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp";

    /**
     * 数据包最大大小（MB）
     */
    public static final String PACKAGE_MAX_SIZE_MB = "design.package.max_size_mb";

    /**
     * 数据包最大大小默认值（500MB）
     */
    public static final int PACKAGE_MAX_SIZE_MB_DEFAULT = 500;

    /**
     * 可视化模型最大大小（MB）
     */
    public static final String MODEL_MAX_SIZE_MB = "design.model.max_size_mb";

    /**
     * 可视化模型最大大小默认值（200MB）
     */
    public static final int MODEL_MAX_SIZE_MB_DEFAULT = 200;

    /**
     * 设计报告最大大小（MB）
     */
    public static final String REPORT_MAX_SIZE_MB = "design.report.max_size_mb";

    /**
     * 设计报告最大大小默认值（50MB）
     */
    public static final int REPORT_MAX_SIZE_MB_DEFAULT = 50;
}
