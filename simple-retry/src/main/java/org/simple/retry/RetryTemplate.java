package org.simple.retry;

import java.util.function.Supplier;

/**
 * 重试模板类 - 提供最简洁的重试API
 */
public class RetryTemplate {
    
    /**
     * 使用默认策略执行重试操作
     */
    public static <T> T execute(Supplier<T> operation) throws Exception {
        return execute(operation, RetryPolicy.defaultRetryPolicy(false));
    }

    /**
     * 使用指定策略执行重试操作
     */
    public static <T> T execute(Supplier<T> operation, RetryPolicy policy) throws Exception {
        return new RetryExecutor(policy).execute(operation, "default");
    }

    /**
     * 快速重试 - 最多重试3次，每次间隔1秒
     */
    public static <T> T quickRetry(Supplier<T> operation) throws Exception {
        RetryPolicy policy = RetryPolicy.builder()
                .maxNumberOfRetries(3)
                .maxDelayTimeMillis(1000)
                .build();
        return execute(operation, policy);
    }

    /**
     * 慢重试 - 最多重试5次，使用指数退避
     */
    public static <T> T slowRetry(Supplier<T> operation) throws Exception {
        RetryPolicy policy = RetryPolicy.builder()
                .maxNumberOfRetries(5)
                .maxDelayTimeMillis(30000)
                .build();
        return execute(operation, policy);
    }
}
