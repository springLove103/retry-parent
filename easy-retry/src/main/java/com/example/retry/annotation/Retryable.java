package com.example.retry.annotation;

import com.example.retry.enums.BackoffStrategy;
import com.example.retry.enums.PersistStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 重试注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {
    
    /**
     * 入库策略
     */
    PersistStrategy persistStrategy() default PersistStrategy.RETRY_ONLY;
    
    /**
     * 最大重试次数
     */
    int maxAttempts() default 3;
    
    /**
     * 最大重试时长(毫秒)
     */
    long maxRetryDuration() default 3600000; // 1小时
    
    /**
     * 退避策略
     */
    BackoffStrategy backoffStrategy() default BackoffStrategy.EXPONENTIAL;
    
    /**
     * 基础延迟时间(毫秒)
     */
    long baseDelay() default 1000;
    
    /**
     * 最大延迟时间(毫秒)
     */
    long maxDelay() default 60000;
    
    /**
     * 需要重试的异常类型
     */
    Class<? extends Exception>[] retryFor() default {};
    
    /**
     * 不需要重试的异常类型
     */
    Class<? extends Exception>[] noRetryFor() default {};
    
    /**
     * 成功条件SpEL表达式
     */
    String successCondition() default "";
    
    /**
     * 业务类型
     */
    String businessType() default "";
}
