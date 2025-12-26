package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.EasyRetryTask;
import org.example.mapper.EasyRetryTaskMapper;
import org.example.service.IEasyRetryTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class EasyRetryTaskServiceImpl extends ServiceImpl<EasyRetryTaskMapper, EasyRetryTask>
        implements IEasyRetryTaskService {



/*    @Override
    public List<EasyRetryTask> getPendingTasks(Integer total, Integer index, Integer size) {
        return baseMapper.selectPendingTasks(total, index, size);
    }

    @Override
    public boolean updateToProcessing(Long id) {
        return baseMapper.updateToProcessing(id) > 0;
    }*/


    @Override
    public List<EasyRetryTask> getPendingTasks(Integer total, Integer index, Integer size) {
        // 注意：MOD(id, total) = index 这种复杂条件需要用 apply
        LambdaQueryWrapper<EasyRetryTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EasyRetryTask::getLockStatus, 1)
                .apply("MOD(id, {0}) = {1}", total, index)
                .last("LIMIT " + size);

        return baseMapper.selectList(wrapper);
    }

    @Override
    public boolean updateToProcessing(Long id) {
        LambdaUpdateWrapper<EasyRetryTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(EasyRetryTask::getLockStatus, 2)
                .set(EasyRetryTask::getGmtModified, LocalDateTime.now())
                .eq(EasyRetryTask::getId, id)
                .eq(EasyRetryTask::getLockStatus, 1);

        return baseMapper.update(null, wrapper) > 0;
    }


}
