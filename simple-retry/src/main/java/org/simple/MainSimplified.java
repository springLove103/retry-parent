package org.simple;

import com.google.gson.Gson;
import okhttp3.*;
import org.simple.http.FormatType;
import org.simple.http.HttpRequest;
import org.simple.http.HttpResponse;
import org.simple.http.MethodType;
import org.simple.retry.RetryExecutor;
import org.simple.retry.RetryPolicy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainSimplified {
    private static final String TARGET_URL = "http://192.168.8.1:8001/notify";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {
        System.out.println("Simple-Retry 框架简化使用示例");
        
        // 使用默认重试策略
        RetryPolicy retryPolicy = RetryPolicy.defaultRetryPolicy(false);
        RetryExecutor retryExecutor = new RetryExecutor(retryPolicy);

        try {
            // 方式1：使用函数式接口简化重试
            String result = retryExecutor.execute(() -> {
                try {
                    return makeHttpCall();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, TARGET_URL);
            
            System.out.println("请求成功: " + result);

        } catch (Exception e) {
            System.err.println("请求失败: " + e.getMessage());
        }

        // 方式2：使用HTTP操作接口
        try {
            HttpResponse response = retryExecutor.executeHttp(new HttpCallOperation(), TARGET_URL);
            System.out.println("HTTP请求成功，状态码: " + response.getStatus());
        } catch (Exception e) {
            System.err.println("HTTP请求失败: " + e.getMessage());
        }
    }

    private static String makeHttpCall() throws IOException {
        Map<String, Object> result = Map.of("orderId", 123);
        Gson gson = new Gson();
        String payload = gson.toJson(result);

        RequestBody requestBody = RequestBody.create(payload.getBytes(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(TARGET_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    static class HttpCallOperation implements RetryExecutor.HttpOperation<HttpResponse> {
        private HttpRequest request;
        private HttpResponse response;

        @Override
        public HttpResponse execute() throws Exception {
            Map<String, Object> result = Map.of("orderId", 123);
            Gson gson = new Gson();
            String payload = gson.toJson(result);

            request = new HttpRequest(TARGET_URL);
            request.setSysMethod(MethodType.POST);
            request.setHttpContent(payload.getBytes(), "UTF-8", FormatType.JSON);

            RequestBody requestBody = RequestBody.create(request.getHttpContent(), MediaType.parse("application/json"));
            Request okRequest = new Request.Builder()
                    .url(request.getSysUrl())
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response okResponse = client.newCall(okRequest).execute()) {
                response = new HttpResponse();
                response.setStatus(okResponse.code());

                if (okResponse.body() != null) {
                    byte[] bodyBytes = okResponse.body().bytes();
                    response.setHttpContent(bodyBytes, "UTF-8", null);
                }

                return response;
            }
        }

        @Override
        public boolean isSuccess() {
            return response != null && response.isSuccess();
        }

        @Override
        public HttpRequest getRequest() {
            return request;
        }

        @Override
        public HttpResponse getResponse() {
            return response;
        }
    }
}
