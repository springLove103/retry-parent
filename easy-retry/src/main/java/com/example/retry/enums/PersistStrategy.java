package com.example.retry.enums;

/**
 * 入库策略枚举
 */
public enum PersistStrategy {
    /**
     * 仅重试入库 - 推荐默认策略
     * 只有需要重试的失败才入库
     */
    RETRY_ONLY,
    
    /**
     * 失败时入库
     * 任何失败都入库(无论是否需要重试)
     */
    ON_FAILURE,
    
    /**
     * 总是入库
     * 方法执行前就入库
     */
    ALWAYS,
    
    /**
     * 手动控制
     * 完全由调用方决定
     */
    MANUAL,
    
    /**
     * 从不入库
     * 纯内存重试
     */
    NEVER
}
