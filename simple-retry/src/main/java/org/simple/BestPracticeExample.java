package org.simple;

import com.google.gson.Gson;
import okhttp3.*;
import org.simple.retry.RetryTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 重试框架最佳实践示例
 */
public class BestPracticeExample {
    private static final String TARGET_URL = "http://192.168.8.1:8001/notify";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {
        System.out.println("=== 重试框架最佳实践示例 ===\n");

        // 示例1: 最简单的重试
//        example1_SimpleRetry();

        // 示例2: 快速重试
//        example2_QuickRetry();

        // 示例3: 慢重试
        example3_SlowRetry();
    }

    /**
     * 示例1: 最简单的重试 - 一行代码搞定
     */
    private static void example1_SimpleRetry() {
        System.out.println("1. 简单重试示例:");
        try {
            String result = RetryTemplate.execute(() -> {
                try {
                    return callApi("org/simple");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("✓ 成功: " + result);
        } catch (Exception e) {
            System.out.println("✗ 失败: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 示例2: 快速重试 - 适用于轻量级操作
     */
    private static void example2_QuickRetry() {
        System.out.println("2. 快速重试示例:");
        try {
            String result = RetryTemplate.quickRetry(() -> {
                try {
                    return callApi("quick");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("✓ 成功: " + result);
        } catch (Exception e) {
            System.out.println("✗ 失败: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 示例3: 慢重试 - 适用于重要的业务操作
     */
    private static void example3_SlowRetry() {
        System.out.println("3. 慢重试示例:");
        try {
            String result = RetryTemplate.slowRetry(() -> {
                try {
                    return callApi("slow");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("✓ 成功: " + result);
            
            // 成功流水日志
            logTransaction("slow_retry", "SUCCESS", result);
            
        } catch (Exception e) {
            System.out.println("✗ 失败: " + e.getMessage());
            
            // 失败流水日志
            logTransaction("slow_retry", "FAILED", e.getMessage());
        }
        System.out.println();
    }

    /**
     * 记录流水日志
     */
    private static void logTransaction(String operation, String status, String details) {
        System.out.println("[流水日志] " + operation + " - " + status + " - " + details);
    }

    /**
     * 模拟API调用
     */
    private static String callApi(String type) throws IOException {
        Map<String, Object> payload = Map.of(
            "type", type,
            "timestamp", System.currentTimeMillis()
        );

        RequestBody body = RequestBody.create(
            new Gson().toJson(payload).getBytes(),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(TARGET_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code());
            }
            return response.body() != null ? response.body().string() : "success";
        }
    }
}
