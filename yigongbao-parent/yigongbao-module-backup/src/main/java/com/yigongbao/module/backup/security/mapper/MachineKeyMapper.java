package com.yigongbao.module.backup.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.security.entity.MachineKeyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 机器密钥元数据 MyBatis-Plus Mapper。 */
@Mapper
public interface MachineKeyMapper extends BaseMapper<MachineKeyEntity> {

    /**
     * 按全局 key_id 查询全部未删除记录。
     *
     * <p>数据库历史表结构允许不同 machine_id 使用相同 key_id，认证层不得
     * 通过 LIMIT 1 随机选中一条记录；调用方会对结果数量做严格的全局唯一性校验。</p>
     */
    @Select("SELECT * FROM backup_machine_key WHERE key_id = #{keyId} AND is_deleted = 0")
    List<MachineKeyEntity> selectByKeyId(String keyId);
}
