package com.yigongbao.module.basic.code.convert;

import com.yigongbao.module.basic.code.dto.CreateCodeRuleDTO;
import com.yigongbao.module.basic.code.dto.UpdateCodeRuleDTO;
import com.yigongbao.module.basic.code.entity.CodeRuleEntity;
import com.yigongbao.module.basic.code.vo.CodeRuleVO;
import org.springframework.beans.BeanUtils;

/**
 * 编码规则转换器
 *
 * @author hanjor
 * @date 2026-03-24
 */
public class CodeRuleConvert {

    /**
     * Entity 转 VO
     */
    public static CodeRuleVO toVO(CodeRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        CodeRuleVO vo = new CodeRuleVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     */
    public static CodeRuleEntity toEntity(CreateCodeRuleDTO dto) {
        if (dto == null) {
            return null;
        }
        CodeRuleEntity entity = new CodeRuleEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /**
     * DTO 转 Entity（更新）
     */
    public static CodeRuleEntity toEntity(UpdateCodeRuleDTO dto) {
        if (dto == null) {
            return null;
        }
        CodeRuleEntity entity = new CodeRuleEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
