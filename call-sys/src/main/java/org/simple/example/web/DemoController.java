package org.simple.example.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
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

    @PostMapping("/notify")
    public Map<String, Object> notify(@RequestBody Map<String, Object> map) {
        log.info("请求参数: {}", map);
        return Map.of("status", "1", "message", "success");
//        return Map.of("status", "200", "message", "success");
    }

}
