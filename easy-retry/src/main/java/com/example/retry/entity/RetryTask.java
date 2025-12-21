package com.example.retry.entity;

import com.example.retry.enums.BackoffStrategy;
import com.example.retry.enums.TaskStatus;

import java.time.LocalDateTime;

/**
 * 重试任务实体
 */
public class RetryTask {
    
    private Long id;
    private String taskKey;
    private String methodName;
    private String paramsJson;
    
    private TaskStatus status;
    private Integer attemptCount;
    
    private Integer maxAttempts;
    private LocalDateTime nextRetryTime;
    private LocalDateTime deadline;
    private Long maxRetryDuration;
    private BackoffStrategy backoffStrategy;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime firstFailedAt;
    
    private String lastErrorMsg;
    private LocalDateTime lastErrorTime;
    
    private Integer version;
    private String lockedBy;
    private LocalDateTime lockedAt;
    
    private String businessId;
    private String businessType;
    
    // 构造函数
    public RetryTask() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    
    public Long getMaxRetryDuration() { return maxRetryDuration; }
    public void setMaxRetryDuration(Long maxRetryDuration) { this.maxRetryDuration = maxRetryDuration; }
    
    public BackoffStrategy getBackoffStrategy() { return backoffStrategy; }
    public void setBackoffStrategy(BackoffStrategy backoffStrategy) { this.backoffStrategy = backoffStrategy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getFirstFailedAt() { return firstFailedAt; }
    public void setFirstFailedAt(LocalDateTime firstFailedAt) { this.firstFailedAt = firstFailedAt; }
    
    public String getLastErrorMsg() { return lastErrorMsg; }
    public void setLastErrorMsg(String lastErrorMsg) { this.lastErrorMsg = lastErrorMsg; }
    
    public LocalDateTime getLastErrorTime() { return lastErrorTime; }
    public void setLastErrorTime(LocalDateTime lastErrorTime) { this.lastErrorTime = lastErrorTime; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
}
