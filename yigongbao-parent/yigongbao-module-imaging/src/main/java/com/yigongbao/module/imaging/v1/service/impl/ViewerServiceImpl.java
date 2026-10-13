package com.yigongbao.module.imaging.v1.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.service.DesignModelService;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.imaging.entity.PartColorEntity;
import com.yigongbao.module.imaging.mapper.PartColorMapper;
import com.yigongbao.module.imaging.v1.service.ViewerService;
import com.yigongbao.module.imaging.v1.vo.StlFileVO;
import com.yigongbao.module.imaging.v1.vo.ViewerConfigVO;
import com.yigongbao.module.imaging.v1.vo.ViewerStlVO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.service.OrderFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 影像查看器适配服务实现（v1）
 *
 * @author hanjor
 * @date 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewerServiceImpl implements ViewerService {

    private static final String CONTEXT_PATH = "/api";
    private static final String DCM_PATH = CONTEXT_PATH + "/imaging/v1/dcm";
    private static final String STL_PATH = CONTEXT_PATH + "/imaging/v1/stl";
    private static final String STL_LIST_PATH = CONTEXT_PATH + "/imaging/v1/stl-list";
    private static final String MARK_PATH = CONTEXT_PATH + "/imaging/v1/mark";

    private final OrderFileService orderFileService;
    private final DesignPackageService designPackageService;
    private final DesignPackageFileService designPackageFileService;
    private final PartColorMapper partColorMapper;
    private final FileService fileService;
    private final DesignScreenshotService screenshotService;
    private final DesignModelService designModelService;

    @Override
    public ViewerConfigVO getViewerConfig(Long orderId, String token) {
        // dcmPath：查看器用此接口获取 DCM 影像压缩包 URL 列表
        ViewerConfigVO.PathItem dcmItem = new ViewerConfigVO.PathItem();
        dcmItem.setPath(DCM_PATH);
        dcmItem.setParams(Map.of("orderId", orderId));
        dcmItem.setType("post");

        // stlPath：查看器用此接口获取 STL 模型数据（按数据包分组）
        ViewerConfigVO.PathItem stlItem = new ViewerConfigVO.PathItem();
        stlItem.setPath(STL_PATH);
        stlItem.setParams(Map.of("orderId", orderId));
        stlItem.setType("post");

        // stlList：查看器用此接口获取 STL 文件列表
        ViewerConfigVO.PathItem stlListItem = new ViewerConfigVO.PathItem();
        stlListItem.setPath(STL_LIST_PATH);
        stlListItem.setParams(Map.of("orderId", orderId));
        stlListItem.setType("get");

        // markPath：查看器提交标注截图时调用，groupId/id 由查看器自动附带（对应 stlPath 返回的 groupId/id）
        ViewerConfigVO.PathItem markItem = new ViewerConfigVO.PathItem();
        markItem.setPath(MARK_PATH);
        markItem.setParams(Map.of());
        markItem.setType("post");

        ViewerConfigVO.Paths paths = new ViewerConfigVO.Paths();
        paths.setDcmPath(dcmItem);
        paths.setStlPath(stlItem);
        paths.setStlList(stlListItem);
        paths.setMarkPath(markItem);

        ViewerConfigVO vo = new ViewerConfigVO();
        vo.setPaths(paths);
        // token 注入到查看器请求 Header，key 为 Authorization
        if (StrUtil.isNotBlank(token)) {
            vo.setToken(Map.of("Authorization", token));
        }
        return vo;
    }

    @Override
    public List<String> getDcmUrls(Long orderId) {
        List<OrderFileEntity> orderFiles = orderFileService.listByOrderIdAndCategory(
                orderId, DictCodeConstants.ORDER_FILE_CATEGORY_DCM);
        if (CollUtil.isEmpty(orderFiles)) {
            return new ArrayList<>();
        }
        List<String> fileIds = orderFiles.stream()
                .map(OrderFileEntity::getFileId)
                .collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));
        return orderFiles.stream()
                .map(of -> fileMap.get(of.getFileId()))
                .filter(f -> f != null && StrUtil.isNotBlank(f.getFileUrl()))
                .map(FileVO::getFileUrl)
                .collect(Collectors.toList());
    }

    @Override
    public ViewerStlVO getStlData(Long orderId) {
        List<DesignPackageEntity> packages = designPackageService.list(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .orderByAsc(DesignPackageEntity::getPackageSeq));

        ViewerStlVO vo = new ViewerStlVO();
        vo.setIsGroup(true);
        if (CollUtil.isEmpty(packages)) {
            vo.setList(new ArrayList<>());
            return vo;
        }

        List<Long> packageIds = packages.stream().map(DesignPackageEntity::getId).collect(Collectors.toList());
        List<DesignPackageFileEntity> allFiles = designPackageFileService.list(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .in(DesignPackageFileEntity::getPackageId, packageIds)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder));

        Map<String, PartColorEntity> colorMap = batchQueryColors(
                allFiles.stream().map(DesignPackageFileEntity::getFileName).collect(Collectors.toList()));
        Map<Long, List<DesignPackageFileEntity>> filesByPackage = allFiles.stream()
                .collect(Collectors.groupingBy(DesignPackageFileEntity::getPackageId));

        List<ViewerStlVO.StlGroupVO> groups = packages.stream().map(pkg -> {
            ViewerStlVO.StlGroupVO group = new ViewerStlVO.StlGroupVO();
            group.setGroupId(pkg.getPackageCode());
            group.setGroupName(FileUtil.mainName(pkg.getFileName()));
            List<DesignPackageFileEntity> pkgFiles = filesByPackage.getOrDefault(pkg.getId(), List.of());
            group.setStls(pkgFiles.stream().map(f -> {
                ViewerStlVO.StlItemVO item = new ViewerStlVO.StlItemVO();
                item.setId(f.getId().toString());
                item.setStlName(f.getFileName());
                item.setUrl(f.getFileUrl());
                PartColorEntity color = colorMap.get(FileUtil.mainName(f.getFileName()));
                if (color != null) {
                    item.setColor(color.getColorCode());
                    item.setOpacity(color.getOpacity());
                }
                return item;
            }).collect(Collectors.toList()));
            return group;
        }).collect(Collectors.toList());

        vo.setList(groups);
        return vo;
    }

    @Override
    public List<StlFileVO> getStlFileList(Long orderId) {
        List<DesignModelEntity> models = designModelService.list(
                new LambdaQueryWrapper<DesignModelEntity>()
                        .eq(DesignModelEntity::getOrderId, orderId)
                        .orderByAsc(DesignModelEntity::getId));
        if (CollUtil.isEmpty(models)) {
            return new ArrayList<>();
        }
        List<String> fileIds = models.stream()
                .map(DesignModelEntity::getFileId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(fileIds)) {
            return new ArrayList<>();
        }
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));
        return models.stream()
                .filter(m -> StrUtil.isNotBlank(m.getFileId()))
                .map(m -> fileMap.get(m.getFileId()))
                .filter(f -> f != null && StrUtil.isNotBlank(f.getFileUrl()))
                .map(f -> {
                    StlFileVO vo = new StlFileVO();
                    vo.setStlName(f.getFileName());
                    vo.setUrl(f.getFileUrl());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void saveMark(String groupId, String modelFileId, MultipartFile file) {
        if (StrUtil.isBlank(groupId) || StrUtil.isBlank(modelFileId)) {
            log.warn("groupId 或 modelFileId 为空，无法关联截图");
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "groupId, modelFileId");
        }
        // 通过 groupId(packageCode) 查询 packageId（packageCode 全局唯一）
        DesignPackageEntity pkg = designPackageService.getOne(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getPackageCode, groupId)
                        .last("LIMIT 1"));
        if (pkg == null) {
            log.warn("数据包不存在, groupId={}", groupId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        // 复用现有的截图服务（关联到 packageId + packageFileId）
        screenshotService.saveScreenshot(pkg.getId(), Long.valueOf(modelFileId), file);
        log.info("保存标注截图: packageId={}, packageFileId={}, fileName={}",
            pkg.getId(), modelFileId, file.getOriginalFilename());
    }

    private Map<String, PartColorEntity> batchQueryColors(List<String> fileNames) {
        if (CollUtil.isEmpty(fileNames)) {
            return Map.of();
        }
        Set<String> partDetails = fileNames.stream().map(FileUtil::mainName).collect(Collectors.toSet());
        return partColorMapper.selectList(
                new LambdaQueryWrapper<PartColorEntity>().in(PartColorEntity::getPartDetail, partDetails))
                .stream()
                .collect(Collectors.toMap(PartColorEntity::getPartDetail, c -> c, (a, b) -> a));
    }
}
