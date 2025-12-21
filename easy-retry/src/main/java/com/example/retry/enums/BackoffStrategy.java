package com.example.retry.enums;

/**
 * 退避策略枚举
 */
public enum BackoffStrategy {
    /**
     * 指数退避 - 重试间隔指数增长
     */
    EXPONENTIAL,
    
    /**
     * 固定间隔 - 每次重试间隔固定
     */
    FIXED,
    
    /**
     * 线性增长 - 重试间隔线性增加
     */
    LINEAR
}
