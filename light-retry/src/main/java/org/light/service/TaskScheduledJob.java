package org.light.service;

import lombok.extern.slf4j.Slf4j;
import org.light.dto.ShardingInfo;
import org.light.entity.EasyRetryTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TaskScheduledJob {

    @Autowired
    private ShardingService shardingService;

    @Autowired
    private IEasyRetryTaskService easyRetryTaskService;

    @Value("${spring.application.name}")
    private String serviceName;

    // 每 1 分钟执行一次
//    @Scheduled(cron = "0 0/1 * * * ?")
    public void run() {
        // 1. 获取分片信息
        ShardingInfo sharding = shardingService.getShardingInfo(serviceName);
        
        if (sharding.getIndex() == -1 || sharding.getTotal() <= 0) {
            log.error("分片计算异常，跳过本次执行");
            return;
        }

        log.info("分片信息：Index={}, Total={}", sharding.getIndex(), sharding.getTotal());

        // 2. 按照分片查询数据
        List<EasyRetryTask> tasks = easyRetryTaskService.getPendingTasks(sharding.getIndex(), sharding.getTotal(), 100);

        for (EasyRetryTask task : tasks) {
            // 3. 尝试抢占任务（防并发双重保险）
            if (easyRetryTaskService.updateToProcessing(task.getId())) {
                try {
                    // 4. 执行业务逻辑
                    process(task);
                    log.info("任务 {} 处理成功", task.getId());
                } catch (Exception e) {
                    log.error("任务 {} 处理失败", task.getId(), e);
                }
            }
        }
    }

    private void process(EasyRetryTask task) {
        // 实际业务逻辑代码
    }
}