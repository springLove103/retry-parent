package org.example;

import com.alibaba.fastjson.JSON;
import com.burukeyou.retry.core.FastRetryBuilder;
import com.burukeyou.retry.core.FastRetryer;
import com.burukeyou.retry.core.policy.FastResultPolicy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.http2.ErrorCode;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
@Slf4j
public class Main {

   static OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();

    public static void main(String[] args) throws ExecutionException, InterruptedException {

//        FastResultPolicy<AsyncNoticeRecordEntity> resultPolicy= result->result.equals("444");


        FastRetryer<AsyncNoticeResultDTO> retryer= FastRetryBuilder.<AsyncNoticeResultDTO>builder()
                .attemptMaxTimes(2)  // 指定最大重试次数
                .waitRetryTime(1, TimeUnit.SECONDS) // 指定下一次重试间隔时间
                .retryIfExceptionOfType(Exception.class) // 指定，当发生指定异常TimeoutException才进行重试
                .retryPolicy(new FastResultPolicy<AsyncNoticeResultDTO>() {
                    @Override
                    public boolean canRetry(AsyncNoticeResultDTO asyncNoticeResultDTO) {
                        //return false;
                        return asyncNoticeResultDTO.getStatus().equals(1);
                    }
                })   // 指定当结果为444是就进行重试
                .build();

        CompletableFuture<AsyncNoticeResultDTO> future=retryer.submit(()->{
/*            System.out.println("重试");
            if(0< 10){
                throw new TimeoutException("test");
            }
            return"444";*/
            FaceCallBackResp resp = new FaceCallBackResp();
            resp.setFaceToken(System.currentTimeMillis() + "");
            resp.setSignerId(999L);
            resp.setContractId(134L);
            resp.setStatus(1L);
            return doCallBackRetry(resp, "http://192.168.8.212:8001/notify");
        });

        AsyncNoticeResultDTO o = future.get();
        System.out.println("结果{}" + o);
    }


    public static AsyncNoticeResultDTO doCallBackRetry(FaceCallBackResp resp, String asyncUrl) {
        if (StringUtils.isBlank(asyncUrl)) {
            log.info("人脸验证结果回调URL为空");
            return null;
        }
        String json = JSON.toJSONString(resp);
        RequestBody body = RequestBody.create(
                json,
                MediaType.get("application/json")
        );
        Request request = new Request.Builder()
                .url(asyncUrl)
                .post(body)
                .build();
        AsyncNoticeRecordEntity asyncNoticeRecord = null;
        AsyncNoticeResultDTO result = null;
        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                log.info("人脸验证结果回调响应体为空 [尝试次数:{}] [URL:{}]", 1, asyncUrl);
                throw new RuntimeException("ErrorCode.RESPONSE_BODY_IS_NULL");
            }
            String respStr = responseBody.string();
            result = JSON.parseObject(respStr, AsyncNoticeResultDTO.class);
            if (result != null && Integer.valueOf(1).equals(result.getStatus())) {
                asyncNoticeRecord = new AsyncNoticeRecordEntity(
                        resp.getContractId(), asyncUrl, respStr,
                        1, true, "已接收通知", 3);
                log.info("人脸验证结果回调成功 [尝试次数:{}] [URL:{}]",  1, asyncUrl);
            } else {
                String errorMsg = result != null ? result.getMessage() : "Invalid response format";
                log.info("人脸验证结果回调失败 [尝试次数:{}] [URL:{}] [响应内容:{}]",  1, asyncUrl, respStr);
                throw new RuntimeException("ErrorCode.CALLBACK_FAIL.getCode(), ErrorCode.CALLBACK_FAIL.getMsg():" + errorMsg);
            }
        } catch (Exception e) {
            log.info("人脸验证结果回调失败 [尝试次数:{}] [URL:{}], {}",  1, asyncUrl, e.getMessage());
            throw new RuntimeException("ErrorCode.CALLBACK_FAIL.getCode(), ErrorCode.CALLBACK_FAIL.getMsg():" + e.getMessage());
        }
        return result;
    }
}