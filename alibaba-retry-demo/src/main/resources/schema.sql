-- easy-retry 任务表（从库 README 复制）
CREATE TABLE IF NOT EXISTS easy_retry_task (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               gmt_create DATETIME NOT NULL,
                                               gmt_modified DATETIME NOT NULL,
                                               sharding VARCHAR(64) DEFAULT NULL,
    biz_id VARCHAR(64) DEFAULT NULL,
    executor_name VARCHAR(512) NOT NULL,
    executor_method_name VARCHAR(512) NOT NULL,
    retry_status TINYINT NOT NULL,
    args_str VARCHAR(7168) DEFAULT NULL,
    ext_attrs VARCHAR(3000) DEFAULT NULL
    );