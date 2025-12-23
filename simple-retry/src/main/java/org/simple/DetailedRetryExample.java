package org.simple;

import org.simple.retry.RetryCallback;
import org.simple.retry.RetryExecutor;
import org.simple.retry.RetryPolicy;

/**
 * 详细重试过程记录示例
 */
public class DetailedRetryExample {

    public static void main(String[] args) {
        System.out.println("=== 详细重试过程记录示例 ===\n");

        // 创建重试策略
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxNumberOfRetries(3)
                .maxDelayTimeMillis(5000)
                .build();

        // 创建重试回调
        RetryCallback callback = new DetailedRetryCallback();

        // 创建带回调的重试执行器
        RetryExecutor executor = new RetryExecutor(retryPolicy, callback);

        try {
            String result = executor.execute(() -> {
                // 模拟可能失败的操作
                if (Math.random() < 0.7) { // 70%概率失败
                    throw new RuntimeException("模拟业务异常");
                }
                return "操作成功";
            }, "test_operation");

            System.out.println("最终结果: " + result);

        } catch (Exception e) {
            System.out.println("最终失败: " + e.getMessage());
        }
    }

    /**
     * 详细重试回调实现
     */
    static class DetailedRetryCallback implements RetryCallback {

        @Override
        public void onRetryStart(String coordinate, int attemptNumber) {
            System.out.println("[重试开始] " + coordinate + " - 第" + attemptNumber + "次尝试");
        }

        @Override
        public void onRetrySuccess(String coordinate, int attemptNumber, Object result) {
            System.out.println("[重试成功] " + coordinate + " - 第" + attemptNumber + "次尝试成功，结果: " + result);
        }

        @Override
        public void onRetryFailure(String coordinate, int attemptNumber, Exception exception) {
            System.out.println("[重试失败] " + coordinate + " - 第" + attemptNumber + "次尝试失败，原因: " + exception.getMessage());
        }

        @Override
        public void onFinalSuccess(String coordinate, int totalAttempts, Object result) {
            System.out.println("[最终成功] " + coordinate + " - 总共尝试" + totalAttempts + "次，最终成功");
        }

        @Override
        public void onFinalFailure(String coordinate, int totalAttempts, Exception exception) {
            System.out.println("[最终失败] " + coordinate + " - 总共尝试" + totalAttempts + "次，最终失败");
        }
    }
}
