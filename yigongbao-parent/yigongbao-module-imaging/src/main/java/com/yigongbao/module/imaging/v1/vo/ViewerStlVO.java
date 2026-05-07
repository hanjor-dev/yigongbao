package com.yigongbao.module.imaging.v1.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 查看器STL数据响应 VO
 *
 * @author hanjor
 * @date 2026-05-06
 */
@Data
public class ViewerStlVO {

    private Boolean isGroup;
    private List<StlGroupVO> list;

    @Data
    public static class StlGroupVO {
        private String groupId;
        private String groupName;
        private List<StlItemVO> stls;
    }

    @Data
    public static class StlItemVO {
        private String id;
        private String stlName;
        private String url;
        private String color;
        private BigDecimal opacity;
    }
}
