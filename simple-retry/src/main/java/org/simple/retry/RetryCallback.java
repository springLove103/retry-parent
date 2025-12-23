package org.simple.retry;

/**
 * 重试回调接口 - 用于记录重试过程
 */
public interface RetryCallback {
    
    /**
     * 重试开始前调用
     */
    void onRetryStart(String coordinate, int attemptNumber);
    
    /**
     * 重试成功时调用
     */
    void onRetrySuccess(String coordinate, int attemptNumber, Object result);
    
    /**
     * 重试失败时调用
     */
    void onRetryFailure(String coordinate, int attemptNumber, Exception exception);
    
    /**
     * 最终成功时调用
     */
    void onFinalSuccess(String coordinate, int totalAttempts, Object result);
    
    /**
     * 最终失败时调用
     */
    void onFinalFailure(String coordinate, int totalAttempts, Exception exception);
}
