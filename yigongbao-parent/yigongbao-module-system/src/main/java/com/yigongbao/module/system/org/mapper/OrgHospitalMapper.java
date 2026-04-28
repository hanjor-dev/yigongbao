package com.yigongbao.module.system.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.org.entity.OrgHospitalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 经销商-医院关联 Mapper
 *
 * @author hanjor
 * @date 2026-04-28
 */
@Mapper
public interface OrgHospitalMapper extends BaseMapper<OrgHospitalEntity> {
    @Select("SELECT hospital_org_id FROM sys_org_hospital WHERE distributor_org_id = #{distributorOrgId}")
    List<Long> selectHospitalOrgIdsByDistributorId(@Param("distributorOrgId") Long distributorOrgId);
}
