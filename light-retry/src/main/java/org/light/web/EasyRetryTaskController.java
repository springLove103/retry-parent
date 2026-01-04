package org.light.web;

import org.light.entity.EasyRetryTask;
import org.light.service.IEasyRetryTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/retry-task")
public class EasyRetryTaskController {


    @Autowired
    private IEasyRetryTaskService easyRetryTaskService;


    @PostMapping("/save")
    public void save(@RequestBody EasyRetryTask task) {
        easyRetryTaskService.saveTask(task);
    }

    @PostMapping("/listByPending")
    public List<EasyRetryTask> listByPending() {
        return easyRetryTaskService.listByPending();
    }
}
