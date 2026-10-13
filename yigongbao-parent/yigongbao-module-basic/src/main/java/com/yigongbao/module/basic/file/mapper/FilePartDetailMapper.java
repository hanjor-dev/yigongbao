package com.yigongbao.module.basic.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.file.entity.FilePartDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分片信息 Mapper
 * 仅在手动分片上传（大文件断点续传）时使用
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Mapper
public interface FilePartDetailMapper extends BaseMapper<FilePartDetail> {
}
