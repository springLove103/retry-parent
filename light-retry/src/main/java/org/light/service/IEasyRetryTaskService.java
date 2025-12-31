package org.light.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.light.entity.EasyRetryTask;

import java.util.List;

public interface IEasyRetryTaskService extends IService<EasyRetryTask> {


    /**
     * 查询待处理任务（分片）
     */
    List<EasyRetryTask> getPendingTasks(Integer total, Integer index, Integer size);

    /**
     * 更新任务状态为处理中
     */
    boolean updateToProcessing(Long id);


    List<EasyRetryTask> listByPending();

    void saveTask(EasyRetryTask task);
}
