package com.yigongbao.module.basic.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 机构和用户医院关联轻量查询 Mapper（module-basic 内部使用，避免循环依赖）
 *
 * @author hanjor
 * @date 2026-04-28
 */
@Mapper
public interface OrgQueryMapper {

    @Select("SELECT id, org_name, org_code, org_type FROM sys_org WHERE id = #{id} AND is_deleted = 0")
    Map<String, Object> selectOrgById(@Param("id") Long id);

    @Select("<script>SELECT id, org_name, org_type FROM sys_org WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " AND is_deleted = 0</script>")
    List<Map<String, Object>> selectOrgByIds(@Param("ids") List<Long> ids);

    @Select("<script>SELECT hospital_id FROM sys_user_hospital WHERE hospital_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " GROUP BY hospital_id</script>")
    List<Long> selectAssignedHospitalIds(@Param("ids") List<Long> ids);
}
