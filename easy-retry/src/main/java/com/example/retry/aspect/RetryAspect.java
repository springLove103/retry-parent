package com.example.retry.aspect;

import com.example.retry.annotation.Retryable;
import com.example.retry.core.RetryableTask;
import com.example.retry.dao.RetryTaskDao;
import com.example.retry.entity.RetryTask;
import com.example.retry.enums.PersistStrategy;
import com.example.retry.service.RetryTaskService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 重试切面
 */
@Aspect
@Component
public class RetryAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryAspect.class);
    
    @Autowired
    private RetryTaskService retryTaskService;
    
    @Autowired
    private RetryTaskDao  retryTaskDao;
    
    @Around("@annotation(retryable)")
    public Object handleRetry(ProceedingJoinPoint joinPoint, Retryable retryable) throws Throwable {
        Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        
        // 生成任务key用于防重
        String taskKey = retryTaskService.generateTaskKey(method, args);
        
        RetryTask task = null;
        
        // 根据入库策略决定是否预先创建任务
        if (retryable.persistStrategy() == PersistStrategy.ALWAYS) {
            task = retryTaskService.createRetryTask(method, args, retryable);
            try {
                retryTaskDao.insert(task);
                logger.info("预先创建重试任务: {}", taskKey);
            } catch (Exception e) {
                logger.error("预先创建重试任务失败: {}", taskKey, e);
            }
        }
        
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 判断执行结果
            boolean success = isSuccess(joinPoint.getTarget(), result, retryable);
            
            if (success) {
                // 成功处理
                handleSuccess(joinPoint.getTarget(), result, task);
                return result;
            } else {
                // 失败处理
                return handleFailure(joinPoint.getTarget(), method, args, retryable, 
                    new RuntimeException("方法执行结果不满足成功条件"), task);
            }
            
        } catch (Exception e) {
            // 异常处理
            return handleFailure(joinPoint.getTarget(), method, args, retryable, e, task);
        }
    }
    
    /**
     * 判断执行结果是否成功
     */
    @SuppressWarnings("unchecked")
    private boolean isSuccess(Object target, Object result, Retryable config) {
        // 如果目标对象实现了RetryableTask接口
        if (target instanceof RetryableTask) {
            return ((RetryableTask<Object>) target).isSuccess(result);
        }
        
        // TODO: 支持SpEL表达式判断
        if (!config.successCondition().isEmpty()) {
            // 这里需要实现SpEL表达式解析
            logger.warn("SpEL表达式判断暂未实现: {}", config.successCondition());
        }
        
        // 默认没有异常就是成功
        return true;
    }
    
    /**
     * 处理成功情况
     */
    @SuppressWarnings("unchecked")
    private void handleSuccess(Object target, Object result, RetryTask task) {
        // 触发成功回调
        if (target instanceof RetryableTask) {
            try {
                ((RetryableTask<Object>) target).onSuccess(result);
            } catch (Exception e) {
                logger.error("执行成功回调失败", e);
            }
        }
        
        // 删除重试任务记录
        if (task != null) {
            try {
                retryTaskDao.deleteById(task.getId());
                logger.info("删除成功的重试任务: {}", task.getTaskKey());
            } catch (Exception e) {
                logger.error("删除成功任务失败: {}", task.getTaskKey(), e);
            }
        }
    }
    
    /**
     * 处理失败情况
     */
    private Object handleFailure(Object target, Method method, Object[] args, 
                               Retryable config, Exception exception, RetryTask task) throws Throwable {
        
        // 判断是否需要重试
        boolean needRetry = retryTaskService.shouldRetry(exception, config);
        
        // 根据入库策略处理
        PersistStrategy strategy = config.persistStrategy();
        
        if (strategy == PersistStrategy.NEVER) {
            // 纯内存重试
            return handleMemoryRetry(target, method, args, config, exception);
        }
        
        if (strategy == PersistStrategy.RETRY_ONLY && !needRetry) {
            // 不需要重试且策略是仅重试入库，直接抛出异常
            throw exception;
        }
        
        if (strategy == PersistStrategy.ON_FAILURE || 
            (strategy == PersistStrategy.RETRY_ONLY && needRetry)) {
            
            if (task == null) {
                task = retryTaskService.createRetryTask(method, args, config);
            }
            
            // 更新任务信息
            task.setLastErrorMsg(exception.getMessage());
            task.setLastErrorTime(java.time.LocalDateTime.now());
            
            if (needRetry && !retryTaskService.shouldStopRetry(task)) {
                // 需要重试且未达到停止条件
                task.setNextRetryTime(retryTaskService.calculateNextRetryTime(task, 
                    config.baseDelay(), config.maxDelay()));
                try {
                    if (task.getId() == null) {
                        retryTaskDao.insert(task);
                    } else {
                        retryTaskDao.update(task);
                    }
                    logger.info("创建/更新重试任务: {}", task.getTaskKey());
                } catch (Exception e) {
                    logger.error("保存重试任务失败: {}", task.getTaskKey(), e);
                }
            } else {
                // 最终失败
                handleFinalFailure(target, exception, task);
            }
        }
        
        throw exception;
    }
    
    /**
     * 处理内存重试
     */
    private Object handleMemoryRetry(Object target, Method method, Object[] args, 
                                   Retryable config, Exception lastException) throws Throwable {
        
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            try {
                Thread.sleep(config.baseDelay() * attempt);
                return method.invoke(target, args);
            } catch (Exception e) {
                lastException = e;
                if (attempt == config.maxAttempts()) {
                    break;
                }
            }
        }
        
        // 最终失败
        handleFinalFailure(target, lastException, null);
        throw lastException;
    }
    
    /**
     * 处理最终失败
     */
    @SuppressWarnings("unchecked")
    private void handleFinalFailure(Object target, Exception exception, RetryTask task) {
        // 触发失败回调
        if (target instanceof RetryableTask) {
            try {
                ((RetryableTask<Object>) target).onFinalFailure(exception);
            } catch (Exception e) {
                logger.error("执行失败回调失败", e);
            }
        }
        
        // 删除重试任务记录
        if (task != null) {
            try {
                retryTaskDao.deleteById(task.getId());
                logger.info("删除最终失败的重试任务: {}", task.getTaskKey());
            } catch (Exception e) {
                logger.error("删除失败任务失败: {}", task.getTaskKey(), e);
            }
        }
    }
}
