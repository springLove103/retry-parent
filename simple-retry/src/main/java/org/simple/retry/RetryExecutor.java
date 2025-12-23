package org.simple.retry;

import java.util.function.Supplier;

/**
 * 重试执行器 - 封装重试逻辑，简化框架使用
 */
public class RetryExecutor {
    private final RetryPolicy retryPolicy;
    private RetryCallback callback;

    public RetryExecutor(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public RetryExecutor(RetryPolicy retryPolicy, RetryCallback callback) {
        this.retryPolicy = retryPolicy;
        this.callback = callback;
    }

    /**
     * 执行带重试的操作
     */
    public <T> T execute(Supplier<T> operation, String coordinate) throws Exception {
        int retryCount = 0;
        Exception lastException = null;

        do {
            if (callback != null) {
                callback.onRetryStart(coordinate, retryCount + 1);
            }

            try {
                T result = operation.get();
                
                if (callback != null) {
                    callback.onRetrySuccess(coordinate, retryCount + 1, result);
                    callback.onFinalSuccess(coordinate, retryCount + 1, result);
                }
                
                return result;

            } catch (Exception e) {
                lastException = e;
                
                if (callback != null) {
                    callback.onRetryFailure(coordinate, retryCount + 1, e);
                }

                RetryPolicyContext context = RetryPolicyContext.builder()
                        .coordinate(coordinate)
                        .exception(e)
                        .retriesAttempted(retryCount)
                        .build();

                if (!retryPolicy.shouldRetry(context)) {
                    if (callback != null) {
                        callback.onFinalFailure(coordinate, retryCount + 1, e);
                    }
                    throw e;
                }

                int delay = retryPolicy.getBackoffDelay(context);
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                
                retryCount++;
            }

        } while (retryCount <= retryPolicy.maxNumberOfRetries());

        if (callback != null) {
            callback.onFinalFailure(coordinate, retryCount, lastException);
        }
        
        if (lastException != null) {
            throw lastException;
        }
        
        throw new RuntimeException("操作在 " + retryCount + " 次尝试后失败");
    }

    /**
     * 执行带重试的HTTP操作
     */
    public <T> T executeHttp(HttpOperation<T> httpOperation, String coordinate) throws Exception {
        int retryCount = 0;
        Exception lastException = null;

        do {
            if (callback != null) {
                callback.onRetryStart(coordinate, retryCount + 1);
            }

            try {
                T result = httpOperation.execute();
                
                RetryPolicyContext context = RetryPolicyContext.builder()
                        .coordinate(coordinate)
                        .httpRequest(httpOperation.getRequest())
                        .httpResponse(httpOperation.getResponse())
                        .retriesAttempted(retryCount)
                        .build();

                if (httpOperation.isSuccess() || !retryPolicy.shouldRetry(context)) {
                    if (callback != null) {
                        callback.onRetrySuccess(coordinate, retryCount + 1, result);
                        callback.onFinalSuccess(coordinate, retryCount + 1, result);
                    }
                    return result;
                }

                if (callback != null) {
                    callback.onRetryFailure(coordinate, retryCount + 1, new RuntimeException("HTTP请求失败"));
                }

                int delay = retryPolicy.getBackoffDelay(context);
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                
                retryCount++;

            } catch (Exception e) {
                lastException = e;
                
                if (callback != null) {
                    callback.onRetryFailure(coordinate, retryCount + 1, e);
                }

                RetryPolicyContext context = RetryPolicyContext.builder()
                        .coordinate(coordinate)
                        .httpRequest(httpOperation.getRequest())
                        .exception(e)
                        .retriesAttempted(retryCount)
                        .build();

                if (!retryPolicy.shouldRetry(context)) {
                    if (callback != null) {
                        callback.onFinalFailure(coordinate, retryCount + 1, e);
                    }
                    throw e;
                }

                int delay = retryPolicy.getBackoffDelay(context);
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                
                retryCount++;
            }

        } while (retryCount <= retryPolicy.maxNumberOfRetries());

        if (callback != null) {
            callback.onFinalFailure(coordinate, retryCount, lastException);
        }
        
        if (lastException != null) {
            throw lastException;
        }
        
        throw new RuntimeException("HTTP操作在 " + retryCount + " 次尝试后失败");
    }

    /**
     * HTTP操作接口
     */
    public interface HttpOperation<T> {
        T execute() throws Exception;
        boolean isSuccess();
        org.simple.http.HttpRequest getRequest();
        org.simple.http.HttpResponse getResponse();
    }
}
