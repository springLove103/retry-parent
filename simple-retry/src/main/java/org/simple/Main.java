package org.simple;


import com.google.gson.Gson;
import okhttp3.*;
import org.simple.http.FormatType;
import org.simple.http.HttpRequest;
import org.simple.http.HttpResponse;
import org.simple.http.MethodType;
import org.simple.retry.RetryPolicy;
import org.simple.retry.RetryPolicyContext;
import org.simple.retry.RetryUtil;
import org.simple.retry.backoff.BackoffStrategy;
import org.simple.retry.backoff.EqualJitterBackoffStrategy;
import org.simple.retry.conditions.ExceptionsCondition;
import org.simple.retry.conditions.RetryCondition;
import org.simple.retry.conditions.StatusCodeCondition;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Main {
    // 目标URL
    private static final String TARGET_URL = "http://192.168.8.1:8001/notify";
    // OkHttpClient实例
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {
        HttpRequest request = new HttpRequest("");
        request.setSysMethod(MethodType.POST);
        System.out.println(request.getSysMethod().toString());
        String string = request.getSysMethod().toString();

        System.out.println("Simple-Retry 重试框架演示 - 使用OkHttpClient");
        System.out.println("目标URL: " + TARGET_URL);

        // 创建自定义的重试策略，包含更多状态码作为可重试错误
        Set<RetryCondition> retryConditions = new HashSet<RetryCondition>();
        Set<Integer> retryableStatusCodes = new HashSet<>(RetryUtil.RETRYABLE_STATUS_CODES);
        retryableStatusCodes.add(HttpURLConnection.HTTP_INTERNAL_ERROR); // 500
        StatusCodeCondition statusCodeCondition = StatusCodeCondition.create(retryableStatusCodes);
        retryConditions.add(statusCodeCondition);

        Set<Class<? extends Exception>> exceptions = new HashSet<Class<? extends Exception>>();
        exceptions.add(SocketTimeoutException.class);
        exceptions.add(IOException.class);
        exceptions.add(Exception.class);
        RetryCondition exceptionsCondition = ExceptionsCondition.create(exceptions);
        retryConditions.add(exceptionsCondition);

        BackoffStrategy backoffStrategy = new EqualJitterBackoffStrategy(
                1000, // 基础延迟时间200ms
                RetryUtil.MAX_BACKOFF,
                new Random()
        );

        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxDelayTimeMillis(15 * 1000) // 最大重试延迟时间15秒
                .backoffStrategy(backoffStrategy)
                .maxNumberOfRetries(2)
                .retryConditions(retryConditions)
                .build();

        // 发送带重试机制的HTTP请求
        try {
            Map<String, Object> result = Map.of("orderId", 123);
            Gson gson = new Gson();
            String payload = gson.toJson(result);
            HttpResponse response = makeRequestWithRetry(retryPolicy, TARGET_URL, payload);
            System.out.println("响应状态: " + response.getStatus());
            if (response.getHttpContent() != null) {
                System.out.println("响应内容: " + new String(response.getHttpContent()));
            }
        } catch (Exception e) {
            System.err.println("请求在重试后仍然失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 使用重试机制发送HTTP请求
     * @param retryPolicy 重试策略
     * @param url 请求URL
     * @return HTTP响应
     * @throws Exception 请求异常
     */
    private static HttpResponse makeRequestWithRetry(RetryPolicy retryPolicy, String url, String payload) throws Exception {
        int retryCount = 0;
        Exception lastException = null;

        do {
            try {
                // 创建HTTP请求
                HttpRequest request = new HttpRequest(url);
                request.setSysMethod(MethodType.POST);
                byte[] bytes = payload.getBytes();
                request.setHttpContent(bytes, "UTF-8", FormatType.JSON);

                // 使用OkHttpClient执行实际的HTTP调用
                HttpResponse response = executeHttpRequest(request);

                // 创建重试上下文
                RetryPolicyContext context = RetryPolicyContext.builder()
                        .coordinate(url)
                        .httpRequest(request)
                        .httpResponse(response)
                        .retriesAttempted(retryCount)
                        .build();

                // 检查是否应该重试
                if (response.isSuccess()) {
                    System.out.println("请求成功完成。尝试次数: " + (retryCount + 1));
                    return response;
                }

                if (!retryPolicy.shouldRetry(context)) {
                    System.out.println("请求失败且不符合重试条件。尝试次数: " + (retryCount + 1));
                    System.out.println("响应状态码: " + response.getStatus());
                    return response;
                }

                // 获取下次重试前的延迟时间
                int delay = retryPolicy.getBackoffDelay(context);
                // 确保延迟时间不为负数
                if (delay < 0) {
                    delay = RetryUtil.BASE_DELAY; // 使用默认的基础延迟时间
                }
                System.out.println("请求失败，将在 " + delay + "ms 后重试... 尝试次数: " + (retryCount + 1));

                // 等待后重试
                Thread.sleep(delay);
                retryCount++;

            } catch (Exception e) {
                lastException = e;

                // 创建包含异常的上下文
                HttpRequest request = new HttpRequest(url);
                request.setSysMethod(MethodType.POST);

                RetryPolicyContext context = RetryPolicyContext.builder()
                        .coordinate(url)
                        .httpRequest(request)
                        .exception(e)
                        .retriesAttempted(retryCount)
                        .build();

                // 根据异常检查是否应该重试
                if (!retryPolicy.shouldRetry(context)) {
                    System.out.println("请求失败且不符合重试条件。尝试次数: " + (retryCount + 1));
                    throw e;
                }

                // 获取下次重试前的延迟时间
                int delay = retryPolicy.getBackoffDelay(context);
                // 确保延迟时间不为负数
                if (delay < 0) {
                    delay = RetryUtil.BASE_DELAY; // 使用默认的基础延迟时间
                }
                System.out.println("请求因异常失败，将在 " + delay + "ms 后重试... 尝试次数: " + (retryCount + 1));

                // 等待后重试
                Thread.sleep(delay);
                retryCount++;
            }

            // 检查是否超过最大重试次数
            if (retryCount > retryPolicy.maxNumberOfRetries()) {
                System.out.println("已达到最大重试次数: " + retryPolicy.maxNumberOfRetries());
                break;
            }
        } while (true);

        // 如果重试次数已用完，抛出最后一个异常
        if (lastException != null) {
            throw lastException;
        }

        throw new RuntimeException("请求在 " + retryCount + " 次尝试后失败");
    }

    /**
     * 执行HTTP请求
     * @param request HTTP请求
     * @return HTTP响应
     * @throws IOException IO异常
     */
    private static HttpResponse executeHttpRequest(HttpRequest request) throws IOException {
        // 构建OkHttpClient请求 - 匹配curl命令的行为
        Request.Builder requestBuilder = new Request.Builder()
                .url(request.getSysUrl())
                .addHeader("Content-Type", "application/json");

        RequestBody requestBody = RequestBody.create(request.getHttpContent(), MediaType.parse("application/json"));

        Request okRequest = requestBuilder
                .post(requestBody)  // 明确使用POST方法
                .build();

        // 执行请求
        try (Response response = client.newCall(okRequest).execute()) {
            // 从OkHttp响应创建HttpResponse
            HttpResponse httpResponse = new HttpResponse();
            httpResponse.setStatus(response.code());

            // 设置响应体（如果可用）
            if (response.body() != null) {
                byte[] bodyBytes = response.body().bytes();
                httpResponse.setHttpContent(bodyBytes, "UTF-8", null);
            }

            // 复制响应头
            if (response.headers() != null) {
                for (String name : response.headers().names()) {
                    httpResponse.putHeaderParameter(name, response.header(name));
                }
            }

            return httpResponse;
        }
    }
}
