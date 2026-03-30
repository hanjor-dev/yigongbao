package com.yigongbao.module.system.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.dict.dto.CreateDictDTO;
import com.yigongbao.module.system.dict.dto.UpdateDictDTO;
import com.yigongbao.module.system.dict.entity.DictEntity;
import com.yigongbao.module.system.dict.vo.DictVO;

import java.util.List;

/**
 * 字典 Service 接口
 *
 * @author hanjor
 * @date 2026-03-16
 */
public interface DictService extends IService<DictEntity> {

    /**
     * 字典类型列表（根节点）
     *
     * @return 字典类型列表
     */
    List<DictVO> listType();

    /**
     * 根据类型编码获取字典数据列表
     *
     * @param typeCode 类型编码（根节点的dictCode）
     * @return 字典数据列表
     */
    List<DictVO> listByTypeCode(String typeCode);

    /**
     * 获取完整树形结构
     *
     * @return 树形结构列表
     */
    List<DictVO> listTree();

    /**
     * 获取指定类型的树形结构
     *
     * @param typeCode 类型编码
     * @return 树形结构列表
     */
    List<DictVO> listTreeByTypeCode(String typeCode);

    /**
     * 获取下拉选项（叶子节点）
     *
     * @param typeCode 类型编码
     * @return 叶子节点列表
     */
    List<DictVO> listOptions(String typeCode);

    /**
     * 根据ID查询字典
     *
     * @param id 字典ID
     * @return 字典VO
     */
    DictVO getById(Long id);

    /**
     * 根据字典编码查询字典
     *
     * @param dictCode 字典编码（叶子节点的 dictCode）
     * @return 字典VO，不存在返回 null
     */
    DictVO getByDictCode(String dictCode);

    /**
     * 创建字典
     *
     * @param dto 创建参数
     */
    void create(CreateDictDTO dto);

    /**
     * 更新字典
     *
     * @param id 字典ID
     * @param dto 更新参数
     */
    void update(Long id, UpdateDictDTO dto);

    /**
     * 删除字典
     *
     * @param id 字典ID
     */
    void remove(Long id);

    /**
     * 修改字典状态（级联）
     *
     * @param id 字典ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取文件业务类型下拉列表
     * 查询字典 parentId=50 的所有子节点（status=1），用于前端文件上传 bizType 下拉选择
     *
     * @return 文件业务类型列表（dictName=name, dictCode=value）
     */
    List<DictVO> listFileBizTypeOptions();
}
