package org.example.service;


import com.alibaba.easyretry.common.constant.enums.RetryTypeEnum;
import com.alibaba.easyretry.extension.spring.aop.EasyRetryable;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class MyService {

    private volatile int counter = 0;

    @EasyRetryable
    public void sendToThirdParty(String payload) {
        if (payload.equals("hello")) {
            counter++;
            System.out.println(new Date() + " - 执行 sendToThirdParty，尝试次数 = " + counter + ", payload=" + payload);
            throw new RuntimeException("模拟第三方调用失败");
        }
    }
}