package org.example.web;

import org.example.dto.ShardingInfo;
import org.example.service.ShardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShardingController {

    @Autowired
    private ShardingService shardingService;

    @PostMapping("/sharding")
    public String getShardingInfo() {
        ShardingInfo shardingInfo = shardingService.getShardingInfo();
        return "index: " + shardingInfo.getIndex() + ", total: " + shardingInfo.getTotal();
    }
}
