package com.example.retry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 重试框架配置
 */
@Configuration
@EnableAspectJAutoProxy
@EnableScheduling
public class RetryConfig {
    
    /**
     * 重试任务执行线程池
     */
    @Bean
    public ThreadPoolExecutor retryExecutor() {
        return new ThreadPoolExecutor(
            5,                          // 核心线程数
            20,                         // 最大线程数
            60L,                        // 空闲时间
            TimeUnit.SECONDS,           // 时间单位
            new LinkedBlockingQueue<>(1000), // 队列容量
            r -> {
                Thread t = new Thread(r, "retry-task-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );
    }
}
