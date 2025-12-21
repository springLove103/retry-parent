package org.example.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoController {


    @PostMapping("/demo")
    public Map<String, Object> getDemo(@RequestBody Map<String, Object> map) {
        System.out.println(map);
        if (!map.isEmpty()) {
            throw new RuntimeException("测试异常");
        }

        return Map.of("status", "1", "message", "success");
    }

//    @PostMapping("/demo1")
//    public Result<OrderResp> getDemo1(@RequestBody OrderReq req) {
//        System.out.println(req);
//        return Result.success(new OrderResp());
//    }
}
