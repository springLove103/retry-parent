package org.light.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 锁状态枚举
 */
@Getter
@AllArgsConstructor
public enum LockStatusEnum {

    NONE(0, "无锁"),
    /**
     * 待处理
     */
    PENDING(1, "PENDING"),
    /**
     * 处理中
     */
    PROCESSING(2, "PROCESSING");


    private final Integer code;
    private final String message;

    public static String getMessage(Integer code) {
        for (LockStatusEnum value : LockStatusEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getMessage();
            }
        }
        return null;
    }

    public static LockStatusEnum getByCode(Integer code) {
        for (LockStatusEnum value : LockStatusEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
