package com.example.retry.example;

import com.example.retry.annotation.Retryable;
import com.example.retry.core.RetryableTask;
import com.example.retry.enums.PersistStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.Random;

/**
 * 重试使用示例
 */
@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final Random random = new Random();
    
    /**
     * 示例1: 使用注解方式
     */
    @Retryable(
        persistStrategy = PersistStrategy.RETRY_ONLY,
        maxAttempts = 5,
        retryFor = {SocketTimeoutException.class},
        businessType = "PAYMENT"
    )
    public String processPayment(String orderId, Double amount) throws Exception {
        logger.info("处理支付: orderId={}, amount={}", orderId, amount);
        
        // 模拟网络超时
        if (random.nextBoolean()) {
            throw new SocketTimeoutException("网络超时");
        }
        
        // 模拟业务异常(不重试)
        if (random.nextInt(10) < 2) {
            throw new IllegalArgumentException("订单金额不能为负数");
        }
        
        return "支付成功: " + orderId;
    }
    
    /**
     * 示例2: 实现接口方式
     */
    @Service
    public static class OrderProcessTask implements RetryableTask<String> {
        
        private static final Logger logger = LoggerFactory.getLogger(OrderProcessTask.class);
        private final Random random = new Random();
        
        @Override
        public String execute() throws Exception {
            logger.info("执行订单处理任务");
            
            // 模拟处理逻辑
            if (random.nextBoolean()) {
                throw new RuntimeException("处理失败");
            }
            
            return "ORDER_SUCCESS";
        }
        
        @Override
        public boolean isSuccess(String result) {
            return "ORDER_SUCCESS".equals(result);
        }
        
        @Override
        public void onSuccess(String result) {
            logger.info("订单处理成功: {}", result);
            // 这里可以保存成功记录到业务表
        }
        
        @Override
        public void onFinalFailure(Exception lastError) {
            logger.error("订单处理最终失败", lastError);
            // 这里可以保存失败记录到业务表
        }
    }
}
