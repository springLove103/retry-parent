package org.example.web;

import org.example.dto.ShardingInfo;
import org.example.service.ShardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShardingController {

    @Autowired
    private ShardingService shardingService;

    @Value("${spring.application.name}")
    private String serviceName;

    @PostMapping("/sharding")
    public String getShardingInfo() {
        ShardingInfo shardingInfo = shardingService.getShardingInfo(serviceName);
        return "index: " + shardingInfo.getIndex() + ", total: " + shardingInfo.getTotal();
    }
}
