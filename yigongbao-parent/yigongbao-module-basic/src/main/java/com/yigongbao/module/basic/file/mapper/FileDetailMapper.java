package com.yigongbao.module.basic.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.file.entity.FileDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件记录 Mapper
 * 继承 MyBatis-Plus BaseMapper，提供基础的 CRUD 能力
 * 高级操作（save/update/delete by url）由 FileRecorderService 通过 ServiceImpl 提供
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Mapper
public interface FileDetailMapper extends BaseMapper<FileDetail> {
}
