package com.yigongbao.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.user.entity.UserManagedOrgEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserManagedOrgMapper extends BaseMapper<UserManagedOrgEntity> {

    @Select("SELECT umo.org_id FROM sys_user_managed_org umo " +
            "INNER JOIN sys_org o ON o.id = umo.org_id " +
            "WHERE umo.user_id = #{userId} AND o.is_deleted = 0 AND o.status = 1 " +
            "AND o.org_type IN ('1.2', '1.4') ORDER BY umo.id")
    List<Long> selectOrgIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_managed_org WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
