package com.yigongbao.module.notification.announcement.service;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.notification.announcement.dto.AnnouncementTargetDTO;
import com.yigongbao.module.notification.announcement.mapper.AnnouncementTargetMapper;
import com.yigongbao.module.notification.announcement.vo.AnnouncementTargetUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 发布时将目标描述解析为有效用户快照。 */
@Component
@RequiredArgsConstructor
public class AnnouncementTargetResolver {

    private final AnnouncementTargetMapper targetMapper;

    public List<AnnouncementTargetUserVO> resolve(AnnouncementTargetDTO target) {
        return resolveUsers(target, true);
    }

    /**
     * 估算目标人数；允许当前规则暂时匹配不到有效账户，返回 0 供草稿展示。
     *
     * @param target 全员、角色或指定账户目标规则
     * @return 去重后的有效账户数量
     */
    public int estimate(AnnouncementTargetDTO target) {
        return resolveUsers(target, false).size();
    }

    private List<AnnouncementTargetUserVO> resolveUsers(AnnouncementTargetDTO target, boolean requireNonEmpty) {
        if (target == null || target.getTargetType() == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "公告目标不能为空");
        }
        List<AnnouncementTargetUserVO> users = switch (target.getTargetType()) {
            case ALL -> targetMapper.selectAllActiveUsers();
            case ROLE -> {
                if (CollectionUtils.isEmpty(target.getRoleIds())) {
                    throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "角色目标不能为空");
                }
                yield targetMapper.selectActiveUsersByRoleIds(target.getRoleIds());
            }
            case USER -> {
                if (CollectionUtils.isEmpty(target.getUserIds())) {
                    throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "指定账户不能为空");
                }
                yield targetMapper.selectActiveUsersByIds(target.getUserIds());
            }
        };
        if (requireNonEmpty && CollectionUtils.isEmpty(users)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "公告目标用户为空");
        }
        return users.stream()
                .collect(Collectors.toMap(AnnouncementTargetUserVO::getUserId, Function.identity(), (first, ignored) -> first))
                .values().stream().toList();
    }

    public String operatorName(Long userId) {
        return targetMapper.selectUserDisplayName(userId);
    }
}
