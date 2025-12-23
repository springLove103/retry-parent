package org.example;

import lombok.Getter;
import lombok.Setter;

/**
 * @author yang_yunxiang
 * @date 2020/5/29 13:12
 */
@Getter
@Setter
public class AsyncNoticeResultDTO {


    /**
     * 处理状态
     * 0：成功，其他为失败
     */
    private Integer status;
    /**
     * 消息
     */
    private String message;


    public boolean isSuccess() {
        return status != null && status == 0;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
