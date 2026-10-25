package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.ResultCode;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 团购活动状态机（M2 开团活动）
 *
 * 合法流转：PENDING -> ONGOING -> SUCCESS / FAILED
 * SUCCESS / FAILED 为终态，不可再流转。
 */
public enum ActivityStatus {

    PENDING,
    ONGOING,
    SUCCESS,
    FAILED;

    /** 状态转移表：key=当前状态，value=允许流转到的目标状态集合 */
    private static final Map<ActivityStatus, Set<ActivityStatus>> TRANSFER_TABLE = Map.of(
            PENDING, EnumSet.of(ONGOING),
            ONGOING, EnumSet.of(SUCCESS, FAILED),
            SUCCESS, EnumSet.noneOf(ActivityStatus.class),
            FAILED, EnumSet.noneOf(ActivityStatus.class)
    );

    /** 字符串转枚举，非法值抛业务异常 */
    public static ActivityStatus of(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "活动状态非法: " + value));
    }

    public boolean canTransferTo(ActivityStatus target) {
        return target != null && TRANSFER_TABLE.getOrDefault(this, EnumSet.noneOf(ActivityStatus.class)).contains(target);
    }

    /** 断言合法流转，非法时抛业务异常（2004） */
    public void assertCanTransferTo(ActivityStatus target) {
        if (!canTransferTo(target)) {
            throw new BusinessException(ResultCode.ACTIVITY_STATUS_ERROR,
                    String.format("活动状态不能从 %s 流转到 %s", name(), target == null ? "空" : target.name()));
        }
    }

    /** 终态：已成团 / 已失败 */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
