package com.example.retry.core;

/**
 * 可重试任务接口
 * 实现此接口的类会被框架自动识别并注册
 */
public interface RetryableTask<T> {
    
    /**
     * 执行业务逻辑
     * @return 执行结果
     * @throws Exception 执行异常
     */
    T execute() throws Exception;
    
    /**
     * 判断执行结果是否成功
     * @param result 执行结果
     * @return true表示成功，false表示失败需要重试
     */
    boolean isSuccess(T result);
    
    /**
     * 成功回调
     * @param result 执行结果
     */
    default void onSuccess(T result) {
        // 默认空实现
    }
    
    /**
     * 最终失败回调
     * @param lastError 最后一次异常
     */
    default void onFinalFailure(Exception lastError) {
        // 默认空实现
    }
}
