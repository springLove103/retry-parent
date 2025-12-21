-- 重试任务队列表
CREATE TABLE retry_task (
    -- 核心字段
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_key VARCHAR(255) NOT NULL COMMENT '业务唯一标识',
    method_name VARCHAR(500) NOT NULL COMMENT '方法全限定名',
    params_json TEXT COMMENT '序列化参数(可选加密)',

    -- 状态字段
    status VARCHAR(20) NOT NULL COMMENT 'PENDING/RUNNING',
    attempt_count INT DEFAULT 0 COMMENT '已重试次数',
    
    -- 重试配置
    max_attempts INT NOT NULL COMMENT '最大重试次数',
    next_retry_time TIMESTAMP NOT NULL COMMENT '下次重试时间',
    deadline TIMESTAMP NULL COMMENT '最后重试时间',
    max_retry_duration BIGINT NULL COMMENT '最大重试时长(ms)',
    backoff_strategy VARCHAR(20) DEFAULT 'EXPONENTIAL',
    
    -- 时间字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    first_failed_at TIMESTAMP NULL COMMENT '首次失败时间',
    
    -- 错误信息(最近一次)
    last_error_msg TEXT COMMENT '最后失败原因',
    last_error_time TIMESTAMP NULL,
    
    -- 并发控制
    version INT DEFAULT 0 COMMENT '乐观锁',
    locked_by VARCHAR(100) NULL COMMENT '锁定实例',
    locked_at TIMESTAMP NULL COMMENT '锁定时间',
    
    -- 业务关联
    business_id VARCHAR(100) NULL COMMENT '关联业务ID',
    business_type VARCHAR(50) NULL COMMENT '业务类型',
    
    -- 索引
    UNIQUE KEY uk_task_key (task_key),
    INDEX idx_next_retry (status, next_retry_time),
    INDEX idx_business (business_type, business_id),
    INDEX idx_created (created_at)
) COMMENT='重试任务队列表-只存储待重试任务';
