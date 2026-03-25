package com.yigongbao.module.basic.bodyPart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.bodyPart.dto.CreateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.dto.UpdateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.entity.BodyPartEntity;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartOptionVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartVO;

import java.util.List;

/**
 * 重建部位 Service 接口
 *
 * @author hanjor
 * @date 2026-03-23
 */
public interface BodyPartService extends IService<BodyPartEntity> {

    /**
     * 获取部位树形结构
     *
     * @return 部位树形列表
     */
    List<BodyPartVO> listTree();

    /**
     * 获取部位下拉选项（仅返回启用状态）
     *
     * @return 部位下拉选项列表
     */
    List<BodyPartOptionVO> listOptions();

    /**
     * 查询部位详情
     *
     * @param id 部位ID
     * @return 部位详情
     */
    BodyPartDetailVO getDetailById(Long id);

    /**
     * 创建部位
     *
     * @param dto 创建参数
     */
    void createBodyPart(CreateBodyPartDTO dto);

    /**
     * 更新部位
     *
     * @param id  部位ID
     * @param dto 更新参数
     */
    void updateBodyPart(Long id, UpdateBodyPartDTO dto);

    /**
     * 删除部位
     *
     * @param id 部位ID
     */
    void removeBodyPart(Long id);

    /**
     * 修改部位状态
     *
     * @param id     部位ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);
}
