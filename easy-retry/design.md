分布式可靠重试框架设计方案
一、系统概述
本框架是一个基于Spring的分布式可靠重试解决方案,通过AOP代理和数据库持久化,实现业务方法的自动重试和故障恢复能力。核心特点是失败持久化、灵活配置、调用方可控。
##二、核心设计思路
2.1 任务注册机制
系统启动时,通过以下两种方式识别需要重试的方法:
接口标记方式: 实现 RetryableTask 接口的类,框架自动识别并注册
注解标记方式: 使用 @Retryable 注解标记的方法,通过Spring AOP扫描注册
注册后的方法会被Spring进行动态代理,所有调用都会经过框架的拦截处理。

2.2 任务执行流程
当被代理的方法执行时,框架按以下流程处理:

第一步:方法执行前准备
- 根据配置的入库策略(PersistStrategy)决定是否预先创建任务记录
- 如果策略是 ALWAYS,则立即序列化方法参数并插入数据库,状态为 PENDING
- 生成唯一的任务标识(task_key),用于防重和追踪

第二步:执行目标方法
- 调用实际的业务方法
- 捕获方法执行过程中的异常
- 获取方法返回结果

第三步:判断执行结果
- 成功判断由调用方决定,不是框架硬编码
- 支持两种判断方式:
    - 通过返回值判断(实现 isSuccess 方法)
    - 通过异常判断(抛出异常即为失败)
- 框架不对业务逻辑做任何假设

第四步:根据结果处理

**成功场景:**
- 触发 onSuccess 回调(如果配置了)
- 从 retry_task 表删除记录
- 成功记录由调用方在回调中存储到业务表
- 返回方法执行结果

**失败场景:**
根据入库策略决定是否创建任务记录:
- RETRY_ONLY: 只有需要重试时才入库
- ON_FAILURE: 失败就立即入库(无论是否需要重试)
- MANUAL: 不自动入库,由调用方手动控制
- NEVER: 不入库,纯内存重试

如果需要重试:
- 序列化方法参数、记录异常信息
- 入库或更新 retry_task 记录,状态为 PENDING
- 计算下次重试时间(根据退避策略)

如果达到停止条件(最终失败):
- 触发 onFinalFailure 回调(如果配置了)
- 从 retry_task 表删除记录
- 失败记录由调用方在回调中存储到业务表


2.3 灵活的入库策略与存储分离设计
框架提供五种入库策略,同时将重试任务表与失败记录表分离,让调用方根据业务场景灵活选择:
**核心原则:**
retry_task 表的定位:
- 专注于需要重试的任务
- 只存储 PENDING(待重试) 和 RUNNING(重试中) 状态的任务
- 成功后的任务默认立即删除(可配置保留一段时间)
- 最终失败的任务也会删除

职责分离:
- retry_task 表 = 重试任务队列,关注"待办事项"
- 调用方业务表 = 执行记录存储,关注"历史记录"

---
**RETRY_ONLY(仅重试入库) - 推荐默认策略 ⭐**
入库时机:
- 方法首次执行失败后
- 框架判断该失败"需要重试"时
- 才入 retry_task 表
判断"需要重试"的依据:
- 抛出的异常在 retryFor 白名单中
- 或者不在 noRetryFor 黑名单中
- 或者返回值不满足 successCondition

成功处理:
- 触发回调,从 retry_task 表删除记录
- 成功记录由调用方在回调中存储到业务表

失败处理:
- 需要重试: 入 retry_task 表,状态为PENDING,等待定时任务扫描
- 不需要重试(业务异常): 不入 retry_task 表,直接向上抛出异常
- 最终失败: 触发回调,从 retry_task 表删除记录
- 失败记录由调用方决定存储到业务表
适用场景: 绝大多数重试场景,保持 retry_task 表轻量

示例:
```java
@Retryable(
    persistStrategy = PersistStrategy.RETRY_ONLY,
    retryFor = {SocketTimeoutException.class},  // 网络超时需要重试
    noRetryFor = {BusinessException.class}      // 业务异常不重试
)
public void pay(Order order) {
    // 如果抛出SocketTimeoutException → 入库retry_task
    // 如果抛出BusinessException → 不入库,直接抛给调用方
}
```

---

**ON_FAILURE(失败时入库)**
入库时机: 任何失败都入 retry_task 表(无论是否需要重试)
成功处理:
- 触发回调,从 retry_task 表删除记录
- 成功记录存储到业务表
失败处理:
- 需要重试: 入库retry_task表,状态为PENDING
- 不需要重试(业务异常): 也入库retry_task表,但立即删除
- 最终失败: 触发回调,从 retry_task 表删除记录
与 RETRY_ONLY 的区别:
- RETRY_ONLY: 只在"需要重试"时入库
- ON_FAILURE: 在"任何失败"时都入库,可用于统一监控
适用场景: 需要在重试表中统一追踪所有失败的场景

---

**ALWAYS(总是入库)**
入库时机: 方法执行前就入库,状态为PENDING
成功处理:
- 触发回调
- 从 retry_task 表删除记录(可配置保留一段时间)
- 如需保留执行历史,在回调中存储到业务表或审计表
失败处理:
- 需要重试: 更新retry_task记录,状态保持PENDING,计算next_retry_time
- 最终失败: 触发回调,从 retry_task 表删除记录
适用场景: 需要完整执行轨迹,或有强审计要求
示例: 财务操作、合规审计要求的操作
注意: retry_task表会频繁插入,需要配置积极的清理策略
---

**MANUAL(手动控制)**
入库时机: 完全由调用方通过API决定
成功处理: 调用方决定
失败处理: 调用方决定
适用场景: 复杂业务逻辑,需要细粒度控制
示例: 多步骤事务,只在特定步骤失败时入库

---

**NEVER(从不入库)**
入库时机: 永不入 retry_task 表
成功处理: 无
失败处理: 纯内存重试,应用重启后丢失
适用场景: 轻量级重试、对性能要求极高的场景
示例: 缓存查询、幂等的读操作


策略              入库时机                                      判断时机
RETRY_ONLY        首次失败且判断为"需要重试"时                     首次失败后立即判断
ON_FAILURE       首次失败时(无论是否需要重试)                      首次失败后立即判断
ALWAYS            方法执行前就入库                               不需要判断(总是入库)
MANUAL            调用方手动决定                                 调用方自己判断
NEVER            永不入库                                      不需要判断(不入库)


2.4 重试策略配置
框架支持丰富的重试策略配置:
最大重试次数(maxAttempts)

控制最多重试几次
达到上限后标记为最终失败

最大重试时长(maxRetryDuration)

从任务创建开始计时
超过时长后停止重试,即使未达到重试次数

最后重试时间(deadline)

绝对时间点,不能超过此时间
适用于有时效性的业务(如订单超时)

退避策略(BackoffStrategy)

指数退避: 重试间隔指数增长 (2^n * baseDelay)
固定间隔: 每次重试间隔固定
线性增长: 重试间隔线性增加
支持添加随机抖动,避免惊群效应

2.5 停止重试条件
任务满足以下任一条件时停止重试:

达到最大重试次数
超过最大重试时长(从创建时间算起)
超过最后重试时间(deadline)
任务被手动标记为失败或取消
三个条件是或关系,任何一个满足即停止,给予调用方多维度的控制能力。

2.6 定时扫描与重试
定时任务扫描:
- 后台定时任务(如每5秒)扫描数据库
- 查询条件:
    - 状态为 PENDING
    - next_retry_time 已到达当前时间
    - 未超过停止重试条件

重试执行:
- 使用分布式锁防止多实例重复执行
- 通过乐观锁(version字段)将 PENDING 改为 RUNNING
- 异步执行重试任务,避免阻塞扫描线程
- 反序列化参数,通过反射调用目标方法
- 根据执行结果更新任务状态

执行结果处理:

**重试成功:**
- 触发 onSuccess 回调
- 从 retry_task 表删除记录
- 业务记录由回调存储到业务表

**重试仍然失败但未达停止条件:**
- 状态改回 PENDING
- 更新 attempt_count + 1
- 计算新的 next_retry_time(根据退避策略)
- 记录 last_error_msg 和 last_error_time

**达到停止条件(最终失败):**
- 触发 onFinalFailure 回调
- 从 retry_task 表删除记录
- 业务记录由回调存储到业务表
- 记录详细的错误日志和堆栈信息


2.7 数据库表设计
retry_task 表结构:
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

2.8 retry_task表的状态设计原则

核心理念:
retry_task表是"任务队列",不是"执行日志表"
只有两种状态的原因:
1. **PENDING(待重试)**
    - 任务等待被执行
    - 这是任务的"正常"状态
2. **RUNNING(执行中)**
    - 任务正在被某个实例执行
    - 防止并发重复执行
    - 执行完成后会变回PENDING或被删除

为什么不需要SUCCESS状态:
- 成功的任务已经完成,不需要再重试
- 保留在retry_task表没有意义
- 成功记录应该存储在业务表中
为什么不需要FAILED状态:
- 达到最终失败条件后,任务不再重试
- 保留在retry_task表只会占用空间
- 失败记录应该存储在业务表中

三、关键技术点
3.1 参数序列化
序列化方案:
默认使用JSON序列化Fastjson

注意事项:
参数对象必须可序列化
避免序列化大对象(建议传ID,重试时重新查询)
注意循环引用问题

3.2 幂等性保证
任务级幂等:
- task_key 唯一索引,同一业务操作不会重复入库
- 通过方法名+参数哈希生成唯一标识
- 防止相同任务重复提交到重试队列

执行级幂等:
- 使用乐观锁(version字段)防止并发执行
- 状态流转控制: PENDING -> RUNNING -> (删除记录)
- 通过数据库锁保证同一任务不会被多个实例同时执行

业务级幂等:
- 调用方必须自行保证业务操作的幂等性
- 框架提供 task_id 和 task_key 供调用方使用
- 推荐使用业务唯一键(如订单ID)作为幂等判断依据



3.3 分布式锁
场景:
防止多个应用实例同时扫描并执行同一任务
实现方案:
基于数据库乐观锁(推荐,简单可靠)
锁粒度:
任务级锁: 锁定单个任务,粒度小,并发高
扫描级锁: 锁定整个扫描过程,简单但并发低

3.4 指数退避实现
第1次重试: 2^1 * 1s + 随机(0-1s) = 2-3秒后
第2次重试: 2^2 * 1s + 随机(0-1s) = 4-5秒后
第3次重试: 2^3 * 1s + 随机(0-1s) = 8-9秒后
第n次重试: min(2^n * 1s, 最大间隔) + 随机抖动
随机抖动作用:

避免大量任务同时失败后同时重试(惊群效应)
分散重试时间,降低系统瞬时压力

3.5 成功判断机制
框架提供两种成功判断方式:
方式1: 接口方法判断
javapublic interface RetryableTask<T> {
T execute() throws Exception;
boolean isSuccess(T result); // 调用方实现
}
方式2: 注解配置判断
java@Retryable(
successCondition = "#{result.code == 200}",  // SpEL表达式
retryFor = {TimeoutException.class}          // 哪些异常需要重试
)
默认判断规则:

没有抛出异常 = 成功
抛出 retryFor 中的异常 = 需要重试的失败
抛出其他异常 = 不需要重试的失败


动态配置
支持配置项:

扫描频率: 定时任务的执行间隔
默认重试参数: 最大次数、最大时长
入库策略: 全局默认策略
异常白名单: 哪些异常需要重试

配置方式:
配置文件(application.yml)

4.5 任务去重
去重策略:
基于 task_key 唯一索引
task_key 生成规则: methodName + MD5(params)
相同参数的方法调用只会创建一个任务

重复提交处理:
插入时捕获唯一索引冲突异常
返回已存在任务的ID
可选择更新已存在任务的重试配置

五、使用场景
支付场景
入库策略: ON_FAILURE
重试配置: 最大5次,指数退避,1小时内完成
成功判断: 支付渠道返回成功状态码
停止条件: 订单超时时间(deadline)
