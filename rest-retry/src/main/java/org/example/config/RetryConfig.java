package org.example.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpStatusCodeException;

@Configuration
@EnableRetry
@Slf4j
public class RetryConfig {

    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(MAX_RETRY_ATTEMPTS);
    private final NeverRetryPolicy neverRetryPolicy = new NeverRetryPolicy();

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 1. 重试策略
        ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();
        policy.setExceptionClassifier(configureStatusCodeBasedRetryPolicy());
        retryTemplate.setRetryPolicy(policy);

        // 2. 退避策略：每次间隔 1 秒，方便观察
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(3_000L);
        retryTemplate.setBackOffPolicy(backOff);
        // 3. 监听：打印重试次数
//        retryTemplate.registerListener(new RetryListenerSupport() {
//            @Override
//            public <T, E extends Throwable> void onError(RetryContext ctx,
//                                                         RetryCallback<T, E> callback,
//                                                         Throwable throwable) {
//                log.warn("retry count: {}, ex: {}", ctx.getRetryCount(), throwable.getMessage());
//            }
//        });
        // 3. 注册监听器（函数式，不用继承 Support）
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context,
                                                         RetryCallback<T, E> callback) {
                return true;          // 返回 false 表示不执行本次重试
            }
            @Override
            public <T, E extends Throwable> void onError(RetryContext context,
                                                         RetryCallback<T, E> callback,
                                                         Throwable throwable) {
                log.warn("retry count: {}, ex: {}", context.getRetryCount(), throwable.toString());
            }

            @Override
            public <T, E extends Throwable> void close(RetryContext context,
                                                       RetryCallback<T, E> callback,
                                                       Throwable throwable) {
                // 重试完成后回调，可做统计
            }
        });
        return retryTemplate;
    }

    private Classifier<Throwable, RetryPolicy> configureStatusCodeBasedRetryPolicy() {
        return throwable -> {
            if (throwable instanceof HttpStatusCodeException) {
                HttpStatusCodeException exception = (HttpStatusCodeException) throwable;
                HttpStatusCode statusCode = exception.getStatusCode();
                return getRetryPolicyForStatus(statusCode);
            }
            return simpleRetryPolicy;
        };
    }

    private RetryPolicy getRetryPolicyForStatus(HttpStatusCode statusCode) {
        // 把接口转回枚举，如果值不在枚举范围内则返回 null
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());

        if (httpStatus == null) {          // 非标准状态码，一律不重试
            return neverRetryPolicy;
        }

        return switch (httpStatus) {
            case BAD_GATEWAY,
                 SERVICE_UNAVAILABLE,
                 INTERNAL_SERVER_ERROR,
                 GATEWAY_TIMEOUT -> simpleRetryPolicy;
            default -> neverRetryPolicy;
        };
    }
}
