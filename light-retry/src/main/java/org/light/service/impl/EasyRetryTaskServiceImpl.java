package org.light.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.light.constant.GlobalConstant;
import org.light.constant.LockStatusEnum;
import org.light.constant.RetryStatusEnum;
import org.light.dto.ShardingInfo;
import org.light.entity.EasyRetryTask;
import org.light.mapper.EasyRetryTaskMapper;
import org.light.service.IEasyRetryTaskService;
import org.light.service.ShardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class EasyRetryTaskServiceImpl extends ServiceImpl<EasyRetryTaskMapper, EasyRetryTask>
        implements IEasyRetryTaskService {

    @Autowired
    private ShardingService shardingService;

    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * TODO 测试
     * 获取等待重试的任务
     * @return
     */
    @Override
    public List<EasyRetryTask> listByPending() {

        ShardingInfo shardingInfo = shardingService.getShardingInfo(serviceName);
        // 当前在线的实例总数
        int total = shardingInfo.getTotal();
        // 当前实例index
        int index = shardingInfo.getIndex();
        // GlobalConstant.LOGIC_NODE 逻辑分片64
        // 获取属于当前实例的任务列表
        //[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
        //[22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42]
        //[43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63]
        List<Integer> shards = ShardingService.calculateShards(index, total, GlobalConstant.LOGIC_NODE);

        LambdaQueryWrapper<EasyRetryTask> wrapper = new LambdaQueryWrapper<>();
        // 锁状态-只查询等待的(pending)
        wrapper.eq(EasyRetryTask::getLockStatus, LockStatusEnum.PENDING.getCode());
        // 任务状态-只查询未处理的(init)
        wrapper.eq(EasyRetryTask::getRetryStatus, RetryStatusEnum.INIT.getCode());
        wrapper.in(EasyRetryTask::getSharding, shards);
        wrapper.between(EasyRetryTask::getGmtCreate, LocalDateTime.now().minusDays(30), LocalDateTime.now());

        return baseMapper.selectList(wrapper);
    }

    /**
     * TODO 测试
     * 保存任务
     * @param task
     */
    @Override
    public void saveTask(EasyRetryTask task) {
        String bizId = task.getBizId();
        int shard = Math.abs(bizId.hashCode() % GlobalConstant.LOGIC_NODE);
        task.setSharding(String.valueOf(shard));
        task.setExecutorName("test");
        task.setExecutorMethodName("test");
        task.setRetryStatus(RetryStatusEnum.INIT.getCode());
        task.setLockStatus(LockStatusEnum.PENDING.getCode());
        save(task);
    }
}
