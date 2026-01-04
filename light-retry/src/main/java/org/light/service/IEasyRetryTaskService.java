package org.light.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.light.entity.EasyRetryTask;

import java.util.List;

public interface IEasyRetryTaskService extends IService<EasyRetryTask> {


    List<EasyRetryTask> listByPending();

    void saveTask(EasyRetryTask task);
}
