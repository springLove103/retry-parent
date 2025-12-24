package org.example.controller;

import org.example.service.MyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简单接口，调用会触发被 @EasyRetryable 注解的方法
 */
@RestController
public class DemoController {

    @Autowired
    private MyService myService;

    @GetMapping("/send")
    public String send(@RequestParam(value = "payload", defaultValue = "hello") String payload) {
        myService.sendToThirdParty(payload);
        return "ok";
    }
}
