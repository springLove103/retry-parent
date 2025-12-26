package org.example.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("easy_retry_task")
public class EasyRetryTask implements Serializable {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 数据库分片字段
     */
    private String sharding;

    /**
     * 业务id
     */
    private String bizId;

    /**
     * 执行名称
     */
    private String executorName;

    /**
     * 执行方法名称
     */
    private String executorMethodName;

    /**
     * 重试状态
     */
    private Integer retryStatus;

    /**
     * 执行方法参数
     */
    private String argsStr;

    /**
     * 扩展字段
     */
    private String extAttrs;

    /**
     * 锁状态 0-无锁 1-PENDING 2-PROCESSING
     */
    private Integer lockStatus;
}
