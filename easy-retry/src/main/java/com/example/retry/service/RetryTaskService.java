package com.example.retry.service;

import com.alibaba.fastjson2.JSON;
import com.example.retry.annotation.Retryable;
import com.example.retry.entity.RetryTask;
import com.example.retry.enums.BackoffStrategy;
import com.example.retry.enums.PersistStrategy;
import com.example.retry.enums.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

/**
 * 重试任务服务
 */
@Service
public class RetryTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryTaskService.class);
    private final Random random = new Random();
    
    /**
     * 生成任务唯一标识
     */
    public String generateTaskKey(Method method, Object[] args) {
        try {
            String methodName = method.getDeclaringClass().getName() + "." + method.getName();
            String paramsStr = args != null ? JSON.toJSONString(args) : "";
            String combined = methodName + paramsStr;
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(combined.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return methodName + "_" + sb.toString();
        } catch (Exception e) {
            logger.error("生成任务key失败", e);
            return method.getDeclaringClass().getName() + "." + method.getName() + "_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 创建重试任务
     */
    public RetryTask createRetryTask(Method method, Object[] args, Retryable config) {
        RetryTask task = new RetryTask();
        
        // 基础信息
        task.setTaskKey(generateTaskKey(method, args));
        task.setMethodName(method.getDeclaringClass().getName() + "." + method.getName());
        task.setParamsJson(args != null ? JSON.toJSONString(args) : null);
        
        // 状态信息
        task.setStatus(TaskStatus.PENDING);
        task.setAttemptCount(0);
        
        // 重试配置
        task.setMaxAttempts(config.maxAttempts());
        task.setMaxRetryDuration(config.maxRetryDuration());
        task.setBackoffStrategy(config.backoffStrategy());
        
        // 时间信息
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setNextRetryTime(now);
        
        // 业务信息
        task.setBusinessType(config.businessType());
        
        // 版本控制
        task.setVersion(0);
        
        return task;
    }
    
    /**
     * 判断是否需要重试
     */
    public boolean shouldRetry(Exception exception, Retryable config) {
        if (exception == null) return false;
        
        Class<? extends Exception> exceptionClass = exception.getClass();
        
        // 检查黑名单
        for (Class<? extends Exception> noRetryClass : config.noRetryFor()) {
            if (noRetryClass.isAssignableFrom(exceptionClass)) {
                return false;
            }
        }
        
        // 检查白名单
        if (config.retryFor().length > 0) {
            for (Class<? extends Exception> retryClass : config.retryFor()) {
                if (retryClass.isAssignableFrom(exceptionClass)) {
                    return true;
                }
            }
            return false;
        }
        
        return true; // 默认重试
    }
    
    /**
     * 判断是否达到停止条件
     */
    public boolean shouldStopRetry(RetryTask task) {
        LocalDateTime now = LocalDateTime.now();
        
        // 检查重试次数
        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            return true;
        }
        
        // 检查最大重试时长
        if (task.getMaxRetryDuration() != null && task.getCreatedAt() != null) {
            long duration = java.time.Duration.between(task.getCreatedAt(), now).toMillis();
            if (duration >= task.getMaxRetryDuration()) {
                return true;
            }
        }
        
        // 检查截止时间
        if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 计算下次重试时间
     */
    public LocalDateTime calculateNextRetryTime(RetryTask task, long baseDelay, long maxDelay) {
        long delay;
        
        switch (task.getBackoffStrategy()) {
            case EXPONENTIAL:
                delay = Math.min((long) Math.pow(2, task.getAttemptCount()) * baseDelay, maxDelay);
                break;
            case LINEAR:
                delay = Math.min((task.getAttemptCount() + 1) * baseDelay, maxDelay);
                break;
            case FIXED:
            default:
                delay = baseDelay;
                break;
        }
        
        // 添加随机抖动(0-1秒)
        delay += random.nextInt(1000);
        
        return LocalDateTime.now().plusNanos(delay * 1_000_000);
    }
}
