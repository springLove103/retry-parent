package org.example.controller;

import org.example.service.MyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简单接口，调用会触发被 @EasyRetryable 注解的方法
 */
@RestController
public class DemoController {

    private final MyService myService;

    public DemoController(MyService myService) {
        this.myService = myService;
    }

    @GetMapping("/send")
    public String send(@RequestParam(value = "payload", defaultValue = "hello") String payload) {
        try {
            myService.sendToThirdParty(payload);
            return "ok";
        } catch (Throwable t) {
            // 因为 sendToThirdParty 抛异常，框架会把任务写进表进行持久化重试
            return "triggered retry (exception thrown)";
        }
    }
}
