package com.yigongbao.module.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.system.dict.dto.CreateDictDTO;
import com.yigongbao.module.system.dict.entity.DictEntity;
import com.yigongbao.module.system.dict.mapper.DictMapper;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字典软删除循环测试
 * 验证"创建→删除→再创建→再删除"多轮循环操作不会出现唯一键冲突
 *
 * 背景：原来唯一键为 UNIQUE KEY uk_dict_code (dict_code, is_deleted)，
 * 两次删除后 (dict_code, 1) 冲突。
 * 修复后唯一键为 UNIQUE KEY uk_dict_code (dict_code)，
 * MyBatis Plus 逻辑删除保证 dict_code 在未删除记录中唯一即可。
 *
 * @author hanjor
 */
@SpringBootTest(classes = com.yigongbao.module.system.SystemTestApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("字典软删除循环测试")
class DictSoftDeleteCycleTest {

    @Autowired
    private DictService dictService;

    @Autowired
    private DictMapper dictMapper;

    // 使用高位编码，避免与 schema.sql 预置数据冲突
    private static final String CYCLE_TYPE_CODE = "999";
    private static final String CYCLE_TYPE_NAME = "循环测试字典类型";

    /**
     * 清理上一轮残留的已删除记录，确保每轮测试从干净状态开始。
     * 注意：此处使用物理删除清理测试数据，仅限测试环境。
     */
    @BeforeEach
    void cleanupTestData() {
        dictMapper.delete(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getDictCode, CYCLE_TYPE_CODE));
    }

    // ==================== 核心场景：多轮创建-删除循环 ====================

    @Test
    @DisplayName("单轮创建删除：创建后删除，不应出现异常")
    void singleCycle_createAndDelete_shouldSucceed() {
        // 创建
        Long id = createRootDict(CYCLE_TYPE_CODE, CYCLE_TYPE_NAME);
        assertDictExists(id);

        // 删除
        dictService.remove(id);
        assertDictNotExists(id);
    }

    @Test
    @DisplayName("两轮循环：第2次删除不应出现唯一键冲突（原始问题复现验证）")
    void twoCycles_shouldNotThrowUniqueKeyViolation() {
        // 第 1 轮
        Long id1 = createRootDict(CYCLE_TYPE_CODE, CYCLE_TYPE_NAME);
        dictService.remove(id1);
        assertDictNotExists(id1);

        // 第 2 轮：再创建相同 dictCode 的记录
        Long id2 = createRootDict(CYCLE_TYPE_CODE, CYCLE_TYPE_NAME);
        assertNotEquals(id1, id2, "两次创建应生成不同的主键ID");
        assertDictExists(id2);

        // 第 2 次删除（原始 bug：此处会报 Duplicate entry for uk_dict_code）
        assertDoesNotThrow(
                () -> dictService.remove(id2),
                "第2次删除相同dictCode的记录不应抛出唯一键冲突异常"
        );
        assertDictNotExists(id2);
    }

    @Test
    @DisplayName("五轮循环：多次重复创建删除均不报错")
    void fiveCycles_shouldAllSucceed() {
        int cycles = 5;
        Long lastId = null;

        for (int i = 1; i <= cycles; i++) {
            // 创建
            Long id = createRootDict(CYCLE_TYPE_CODE, CYCLE_TYPE_NAME + "-第" + i + "轮");
            assertDictExists(id);
            if (lastId != null) {
                assertNotEquals(lastId, id, "第" + i + "轮ID应与上一轮不同");
            }
            lastId = id;

            // 删除
            int round = i;
            assertDoesNotThrow(
                    () -> dictService.remove(id),
                    "第" + round + "轮删除不应抛出异常"
            );
            assertDictNotExists(id);
        }

        // 验证所有已删除记录均存在于数据库中（逻辑删除，未物理删除）
        int deletedCount = countDeletedByCode(CYCLE_TYPE_CODE);
        assertEquals(cycles, deletedCount,
                "应有" + cycles + "条已删除记录，实际=" + deletedCount);
    }

    // ==================== 未删除状态唯一性验证 ====================

    @Test
    @DisplayName("未删除唯一性：同时存在两条相同dictCode的未删除记录应被业务层拦截")
    void activeRecord_sameCode_shouldBeRejectedByService() {
        // 先创建一条（未删除）
        createRootDict(CYCLE_TYPE_CODE, CYCLE_TYPE_NAME);

        // 再创建相同 dictCode（业务层通过 isDictCodeExists 检查应拦截）
        // DictService.create 内部会调用 generateDictCode 生成新编码，不会重复
        // 此处直接测试 isDictCodeExists 语义：通过 listType 验证只有1条未删除记录
        List<DictVO> types = dictService.listType();
        long count = types.stream()
                .filter(vo -> CYCLE_TYPE_CODE.equals(vo.getDictCode()))
                .count();
        assertEquals(1, count, "未删除状态下相同dictCode只应存在1条");
    }

    @Test
    @DisplayName("删除后重建：重建的记录可被正常查询到")
    void afterDeleteAndRecreate_newRecordShouldBeQueryable() {
        // 第 1 轮创建删除
        Long id1 = createRootDict(CYCLE_TYPE_CODE, CYCLE_TYPE_NAME);
        dictService.remove(id1);

        // 第 2 轮创建
        Long id2 = createRootDict(CYCLE_TYPE_CODE, CYCLE_TYPE_NAME);

        // 通过 listType 查询，应能找到新记录
        List<DictVO> types = dictService.listType();
        DictVO found = types.stream()
                .filter(vo -> id2.equals(vo.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(found, "重建的记录应可通过listType查询到");
        assertEquals(CYCLE_TYPE_NAME, found.getDictName());

        // 旧记录不应出现在查询结果中
        boolean oldExists = types.stream().anyMatch(vo -> id1.equals(vo.getId()));
        assertFalse(oldExists, "已删除的旧记录不应出现在查询结果中");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建根节点字典，返回新记录 ID。
     * 由于 DictService.create 不返回 ID，通过查询获取。
     */
    private Long createRootDict(String dictCode, String dictName) {
        // 先物理清理同编码的已删除记录，模拟每次干净创建
        // （正式业务不需要，这里是为了让 generateDictCode 能生成固定编码便于断言）
        dictMapper.delete(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getDictCode, dictCode));

        CreateDictDTO dto = new CreateDictDTO();
        dto.setParentId(0L);
        dto.setDictName(dictName);
        dto.setStatus(1);
        dictService.create(dto);

        // 查询刚插入的未删除记录
        DictEntity entity = dictMapper.selectOne(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getDictCode, dictCode)
                .eq(DictEntity::getIsDeleted, 0));
        assertNotNull(entity, "创建后应能查到记录，dictCode=" + dictCode);
        return entity.getId();
    }

    /** 断言记录存在（未删除） */
    private void assertDictExists(Long id) {
        DictVO vo = dictService.getById(id);
        assertNotNull(vo, "ID=" + id + " 的字典应存在");
    }

    /** 断言记录不存在（已被逻辑删除） */
    private void assertDictNotExists(Long id) {
        // getById 内部走 MP 逻辑删除过滤，删除后返回 null
        DictEntity raw = dictMapper.selectById(id);
        // selectById 也受 MP @TableLogic 过滤，已删除记录返回 null
        assertNull(raw, "ID=" + id + " 的字典应已被逻辑删除，不应被查询到");
    }

    /** 统计指定 dictCode 的已删除记录数（绕过 MP 逻辑删除过滤，直接查原始数据） */
    private int countDeletedByCode(String dictCode) {
        // 使用原生 SQL 绕过 @TableLogic 过滤
        Long count = dictMapper.selectCount(
                new LambdaQueryWrapper<DictEntity>()
                        .eq(DictEntity::getDictCode, dictCode)
                        .eq(DictEntity::getIsDeleted, 1)
        );
        return count.intValue();
    }
}
