package org.light.web;

import org.light.constant.GlobalConstant;
import org.light.entity.EasyRetryTask;
import org.light.service.IEasyRetryTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/retry-task")
public class EasyRetryTaskController {


    @Autowired
    private IEasyRetryTaskService easyRetryTaskService;

    @GetMapping("/pending")
    public List<EasyRetryTask> getPendingTasks() {
        // 查询待处理任务（分片索引0，总分片数10，每次取100条）
        return easyRetryTaskService.getPendingTasks(10, 0, 100);
    }

    @PutMapping("/processing/{id}")
    public boolean updateToProcessing(@PathVariable Long id) {
        // 更新任务状态为处理中
        return easyRetryTaskService.updateToProcessing(id);
    }

    @PostMapping("/save")
    public void save(@RequestBody EasyRetryTask task) {
        easyRetryTaskService.saveTask(task);
    }
}
