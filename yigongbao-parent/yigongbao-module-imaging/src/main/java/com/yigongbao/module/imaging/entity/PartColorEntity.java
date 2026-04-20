package com.yigongbao.module.imaging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部位颜色透明度配置表
 * 文件名去扩展名后与 partDetail 精确匹配，获取颜色和透明度
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@TableName("part_colors")
public class PartColorEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 部位名称（与模型文件名精确匹配，去扩展名）
     */
    private String partDetail;

    /**
     * 颜色RGB值（如：170,255,0）
     */
    private String colorCode;

    /**
     * 透明度（0.00~1.00，1=不透明）
     */
    private BigDecimal opacity;
}
