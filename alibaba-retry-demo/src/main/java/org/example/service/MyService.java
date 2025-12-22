package org.example.service;


import com.alibaba.easyretry.extension.spring.aop.EasyRetryable;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class MyService {

    private volatile int counter = 0;

    @EasyRetryable
    public void sendToThirdParty(String payload) {
        counter++;
        System.out.println(new Date() + " - 执行 sendToThirdParty，尝试次数 = " + counter + ", payload=" + payload);
        // 故意抛异常，触发 persistence retry（会写入 retry_task 表）
        throw new RuntimeException("模拟第三方调用失败");
    }
}