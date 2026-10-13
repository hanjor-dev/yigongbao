package com.yigongbao.module.design.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.service.DesignQrImageService;
import com.yigongbao.module.design.vo.DesignQrImageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单图纸二维码图片服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignQrImageServiceImpl implements DesignQrImageService {

    private static final String QR_BIZ_TYPE = FileBizTypeEnum.DRAWING_QR_IMAGE.getDictCode();
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /** 进程内按订单串行替换，跨实例按最后一次成功替换生效。 */
    private static final ConcurrentHashMap<Long, Object> ORDER_LOCKS = new ConcurrentHashMap<>();

    private final FileService fileService;
    private final DesignQueryHelper designQueryHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignQrImageVO upload(Long orderId, MultipartFile file) {
        log.info("收到图纸二维码上传请求，orderId={}, fileName={}, contentType={}, size={}",
                orderId, file == null ? null : file.getOriginalFilename(),
                file == null ? null : file.getContentType(), file == null ? 0 : file.getSize());
        OrderMainEntity order = designQueryHelper.checkDesignPhase(orderId);
        // 不需要校验当前订单所属设计师
        //designQueryHelper.checkIsAssignedDesigner(order);

        byte[] bytes = readAndValidate(file);
        String hash = md5Hex(bytes);
        log.info("图纸二维码文件校验通过，orderId={}, fileName={}, bytes={}, md5={}",
                orderId, file.getOriginalFilename(), bytes.length, hash);

        synchronized (ORDER_LOCKS.computeIfAbsent(orderId, ignored -> new Object())) {
            FileVO current = currentFile(orderId);
            if (current != null && hash.equalsIgnoreCase(current.getFileHash())) {
                log.info("图纸二维码内容未变化，复用已关联文件，orderId={}, fileId={}, md5={}",
                        orderId, current.getId(), hash);
                return toVO(current);
            }

            FileVO uploaded = fileService.uploadFile(file, QR_BIZ_TYPE);
            log.info("图纸二维码文件上传存储完成，orderId={}, fileId={}, bytes={}, md5={}, bizType={}",
                    orderId, uploaded.getId(), bytes.length, hash, QR_BIZ_TYPE);
            try {
                fileService.unlinkByBiz(QR_BIZ_TYPE, orderId);
                fileService.linkFile(uploaded.getId(), QR_BIZ_TYPE, orderId);
                log.info("图纸二维码文件关联完成，orderId={}, fileId={}, objectType={}, objectId={}",
                        orderId, uploaded.getId(), QR_BIZ_TYPE, orderId);
                return toVO(uploaded);
            } catch (RuntimeException ex) {
                log.error("图纸二维码文件关联失败，orderId={}, fileId={}, bizType={}",
                        orderId, uploaded.getId(), QR_BIZ_TYPE, ex);
                restorePreviousAssociation(current, orderId);
                deleteUnlinkedFile(uploaded.getId());
                throw ex;
            }
        }
    }

    @Override
    public DesignQrImageVO getCurrent(Long orderId) {
        designQueryHelper.checkOrderReadable(orderId);
        FileVO current = currentFile(orderId);
        log.info("查询订单当前图纸二维码完成，orderId={}, fileId={}, fileName={}, bytes={}",
                orderId, current == null ? null : current.getId(),
                current == null ? null : current.getFileName(),
                current == null ? null : current.getFileSize());
        return current == null ? null : toVO(current);
    }

    private FileVO currentFile(Long orderId) {
        List<FileVO> files = fileService.listByBiz(QR_BIZ_TYPE, orderId);
        return files == null || files.isEmpty() ? null : files.get(0);
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_SIZE_EXCEEDED);
        }
        String name = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (name == null || !name.toLowerCase().endsWith(".png")
                || !"image/png".equalsIgnoreCase(contentType)) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || !startsWithPngSignature(bytes)) {
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
            }
            return bytes;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    private boolean startsWithPngSignature(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private String md5Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                int unsignedValue = value & 0xff;
                result.append(HEX[unsignedValue >>> 4]);
                result.append(HEX[unsignedValue & 0x0f]);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm is unavailable", ex);
        }
    }

    private void deleteUnlinkedFile(String fileId) {
        if (fileId == null) {
            return;
        }
        try {
            fileService.deleteById(fileId);
        } catch (RuntimeException cleanupEx) {
            log.error("二维码文件关联失败且新文件清理失败，fileId={}", fileId, cleanupEx);
        }
    }

    private void restorePreviousAssociation(FileVO previous, Long orderId) {
        if (previous == null || previous.getId() == null) {
            return;
        }
        try {
            fileService.linkFile(previous.getId(), QR_BIZ_TYPE, orderId);
        } catch (RuntimeException restoreEx) {
            log.error("二维码替换失败且旧文件关联恢复失败，fileId={}, orderId={}",
                    previous.getId(), orderId, restoreEx);
        }
    }

    private DesignQrImageVO toVO(FileVO file) {
        DesignQrImageVO vo = new DesignQrImageVO();
        vo.setFileId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileUrl(file.getFileUrl());
        vo.setDownloadUrl(file.getDownloadUrl());
        vo.setFileSize(file.getFileSize());
        vo.setFileHash(file.getFileHash());
        vo.setUploadTime(file.getCreateTime());
        return vo;
    }
}
