package com.example.retry.scheduler;

import com.alibaba.fastjson2.JSON;
import com.example.retry.dao.RetryTaskDao;
import com.example.retry.entity.RetryTask;
import com.example.retry.enums.TaskStatus;
import com.example.retry.service.RetryTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 重试任务调度器
 */
@Service
public class RetryTaskScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryTaskScheduler.class);
    
    @Autowired
    private RetryTaskDao retryTaskDao;
    
    @Autowired
    private RetryTaskService retryTaskService;
    
    @Autowired(required = false)
    private ThreadPoolExecutor retryExecutor;
    
    private final String instanceId;
    
    public RetryTaskScheduler() {
        try {
            this.instanceId = InetAddress.getLocalHost().getHostName() + "-" + System.currentTimeMillis();
        } catch (Exception e) {
            this.instanceId = "unknown-" + System.currentTimeMillis();
        }
    }
    
    /**
     * 定时扫描待重试任务
     */
    @Scheduled(fixedDelay = 5000) // 每5秒执行一次
    public void scanRetryTasks() {
        try {
            // 释放超时锁定的任务(超过5分钟)
            LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(5);
            int releasedCount = retryTaskDao.releaseTimeoutLocks(timeoutBefore);
            if (releasedCount > 0) {
                logger.info("释放超时锁定任务数量: {}", releasedCount);
            }
            
            // 查询待重试任务
            LocalDateTime now = LocalDateTime.now();
            List<RetryTask> pendingTasks = retryTaskDao.findPendingTasks(now, 100);
            
            if (!pendingTasks.isEmpty()) {
                logger.info("扫描到待重试任务数量: {}", pendingTasks.size());
                
                for (RetryTask task : pendingTasks) {
                    processRetryTask(task);
                }
            }
            
        } catch (Exception e) {
            logger.error("扫描重试任务失败", e);
        }
    }
    
    /**
     * 处理单个重试任务
     */
    private void processRetryTask(RetryTask task) {
        try {
            // 使用乐观锁获取任务执行权
            int updated = retryTaskDao.updateStatusWithVersion(
                task.getId(), 
                TaskStatus.PENDING, 
                TaskStatus.RUNNING, 
                task.getVersion(),
                instanceId
            );
            
            if (updated == 0) {
                // 任务已被其他实例获取
                return;
            }
            
            // 异步执行重试任务
            if (retryExecutor != null) {
                CompletableFuture.runAsync(() -> executeRetryTask(task), retryExecutor);
            } else {
                CompletableFuture.runAsync(() -> executeRetryTask(task));
            }
            
        } catch (Exception e) {
            logger.error("处理重试任务失败: {}", task.getTaskKey(), e);
        }
    }
    
    /**
     * 执行重试任务
     */
    private void executeRetryTask(RetryTask task) {
        try {
            logger.info("开始执行重试任务: {}, 第{}次重试", task.getTaskKey(), task.getAttemptCount() + 1);
            
            // 反序列化参数
            Object[] args = null;
            if (task.getParamsJson() != null) {
                args = JSON.parseArray(task.getParamsJson(), Object.class).toArray();
            }
            
            // 通过反射调用目标方法
            Object result = invokeTargetMethod(task.getMethodName(), args);
            
            // 重试成功
            handleRetrySuccess(task, result);
            
        } catch (Exception e) {
            // 重试失败
            handleRetryFailure(task, e);
        }
    }
    
    /**
     * 通过反射调用目标方法
     */
    private Object invokeTargetMethod(String methodName, Object[] args) throws Exception {
        // 解析方法名
        int lastDotIndex = methodName.lastIndexOf('.');
        String className = methodName.substring(0, lastDotIndex);
        String methodSimpleName = methodName.substring(lastDotIndex + 1);
        
        // 加载类
        Class<?> clazz = Class.forName(className);
        
        // 查找方法
        Method method = null;
        if (args == null || args.length == 0) {
            method = clazz.getDeclaredMethod(methodSimpleName);
        } else {
            // 简化处理，实际应该根据参数类型匹配
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodSimpleName) && m.getParameterCount() == args.length) {
                    method = m;
                    break;
                }
            }
        }
        
        if (method == null) {
            throw new NoSuchMethodException("找不到方法: " + methodName);
        }
        
        // 获取实例（这里简化处理，实际应该从Spring容器获取）
        Object instance = clazz.getDeclaredConstructor().newInstance();
        
        // 调用方法
        return method.invoke(instance, args);
    }
    
    /**
     * 处理重试成功
     */
    private void handleRetrySuccess(RetryTask task, Object result) {
        try {
            logger.info("重试任务执行成功: {}", task.getTaskKey());
            
            // TODO: 触发成功回调
            
            // 删除任务记录
            retryTaskDao.deleteById(task.getId());
            
        } catch (Exception e) {
            logger.error("处理重试成功失败: {}", task.getTaskKey(), e);
        }
    }
    
    /**
     * 处理重试失败
     */
    private void handleRetryFailure(RetryTask task, Exception exception) {
        try {
            logger.warn("重试任务执行失败: {}, 错误: {}", task.getTaskKey(), exception.getMessage());
            
            // 更新失败信息
            task.setAttemptCount(task.getAttemptCount() + 1);
            task.setLastErrorMsg(exception.getMessage());
            task.setLastErrorTime(LocalDateTime.now());
            
            // 判断是否继续重试
            if (retryTaskService.shouldStopRetry(task)) {
                // 达到停止条件，最终失败
                logger.info("重试任务达到停止条件，最终失败: {}", task.getTaskKey());
                
                // TODO: 触发失败回调
                
                // 删除任务记录
                retryTaskDao.deleteById(task.getId());
            } else {
                // 继续重试
                task.setStatus(TaskStatus.PENDING);
                task.setNextRetryTime(retryTaskService.calculateNextRetryTime(task, 1000, 60000));
                task.setLockedBy(null);
                task.setLockedAt(null);
                
                retryTaskDao.update(task);
                
                logger.info("重试任务将在 {} 后重试: {}", task.getNextRetryTime(), task.getTaskKey());
            }
            
        } catch (Exception e) {
            logger.error("处理重试失败失败: {}", task.getTaskKey(), e);
        }
    }
}
