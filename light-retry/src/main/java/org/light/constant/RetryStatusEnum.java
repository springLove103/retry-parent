package org.light.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 重试状态枚举
 */
@Getter
@AllArgsConstructor
public enum RetryStatusEnum {

    /**
     * 初始化
     */
    INIT(0, "初始化"),
    /**
     * 处理中
     */
    PROCESSING(1, "处理中"),
    /**
     * 处理异常
     */
    FAILURE(2, "处理异常"),
    /**
     * 任务完结
     */
    SUCCESS(3, "任务完结");


    private final Integer code;
    private final String message;

    public static String getMessage(Integer code) {
        for (RetryStatusEnum value : RetryStatusEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getMessage();
            }
        }
        return null;
    }

    public static RetryStatusEnum getByCode(Integer code) {
        for (RetryStatusEnum value : RetryStatusEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
