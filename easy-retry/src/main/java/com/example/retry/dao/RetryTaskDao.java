package com.example.retry.dao;

import com.example.retry.entity.RetryTask;
import com.example.retry.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 重试任务数据访问接口
 */
public interface RetryTaskDao {
    
    /**
     * 插入重试任务
     */
    int insert(RetryTask task);
    
    /**
     * 根据taskKey查询任务
     */
    RetryTask findByTaskKey(String taskKey);
    
    /**
     * 更新任务
     */
    int update(RetryTask task);
    
    /**
     * 删除任务
     */
    int deleteById(Long id);
    
    /**
     * 查询待重试的任务
     */
    List<RetryTask> findPendingTasks(LocalDateTime beforeTime, int limit);
    
    /**
     * 乐观锁更新任务状态
     */
    int updateStatusWithVersion(Long id, TaskStatus fromStatus, TaskStatus toStatus, 
                               Integer fromVersion, String lockedBy);
    
    /**
     * 释放超时锁定的任务
     */
    int releaseTimeoutLocks(LocalDateTime timeoutBefore);
}
