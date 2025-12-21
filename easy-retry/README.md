# Easy-Retry 分布式可靠重试框架

基于Spring的分布式可靠重试解决方案，通过AOP代理和数据库持久化，实现业务方法的自动重试和故障恢复能力。

## 核心特性

- **失败持久化**: 支持多种入库策略，确保重试任务不丢失
- **灵活配置**: 丰富的重试策略和停止条件配置
- **调用方可控**: 成功/失败判断和回调处理由调用方决定
- **分布式支持**: 基于数据库乐观锁的分布式任务调度
- **高性能**: 异步执行，支持并发重试

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>easy-retry</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 创建数据库表

```sql
-- 执行 src/main/resources/schema.sql 中的建表语句
```

### 3. 配置Spring

```java
@Configuration
@ComponentScan("com.example.retry")
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        // 配置数据源
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### 4. 使用注解方式

```java
@Service
public class PaymentService {
    
    @Retryable(
        persistStrategy = PersistStrategy.RETRY_ONLY,
        maxAttempts = 5,
        retryFor = {SocketTimeoutException.class},
        noRetryFor = {BusinessException.class}
    )
    public String processPayment(String orderId, Double amount) throws Exception {
        // 业务逻辑
        return "支付成功";
    }
}
```

### 5. 使用接口方式

```java
@Service
public class OrderTask implements RetryableTask<String> {
    
    @Override
    public String execute() throws Exception {
        // 业务逻辑
        return "ORDER_SUCCESS";
    }
    
    @Override
    public boolean isSuccess(String result) {
        return "ORDER_SUCCESS".equals(result);
    }
    
    @Override
    public void onSuccess(String result) {
        // 成功回调
    }
    
    @Override
    public void onFinalFailure(Exception lastError) {
        // 失败回调
    }
}
```

## 入库策略

### RETRY_ONLY (推荐默认)
- 只有需要重试的失败才入库
- 保持retry_task表轻量
- 适用于绝大多数场景

### ON_FAILURE
- 任何失败都入库(无论是否需要重试)
- 可用于统一监控所有失败

### ALWAYS
- 方法执行前就入库
- 提供完整执行轨迹
- 适用于审计要求高的场景

### MANUAL
- 完全由调用方控制
- 适用于复杂业务逻辑

### NEVER
- 纯内存重试
- 高性能，但应用重启后丢失

## 重试策略配置

```java
@Retryable(
    maxAttempts = 5,                    // 最大重试次数
    maxRetryDuration = 3600000,         // 最大重试时长(1小时)
    backoffStrategy = BackoffStrategy.EXPONENTIAL, // 退避策略
    baseDelay = 1000,                   // 基础延迟(1秒)
    maxDelay = 60000                    // 最大延迟(1分钟)
)
```

## 停止重试条件

任务满足以下任一条件时停止重试：
- 达到最大重试次数
- 超过最大重试时长
- 超过最后重试时间(deadline)

## 架构设计

### 核心组件

- **RetryAspect**: AOP切面，拦截方法调用
- **RetryTaskService**: 重试任务服务，处理核心逻辑
- **RetryTaskScheduler**: 定时调度器，扫描并执行重试任务
- **RetryTaskDao**: 数据访问层，管理任务持久化

### 执行流程

1. **方法拦截**: AOP切面拦截@Retryable方法
2. **任务创建**: 根据入库策略决定是否创建任务记录
3. **执行判断**: 调用方决定成功/失败条件
4. **重试调度**: 定时任务扫描并异步执行重试
5. **状态管理**: 乐观锁保证分布式环境下的并发安全

### 数据库设计

retry_task表只存储两种状态的任务：
- **PENDING**: 待重试
- **RUNNING**: 执行中

成功和最终失败的任务会被删除，保持表的轻量。

## 最佳实践

1. **幂等性**: 确保业务方法具备幂等性
2. **参数序列化**: 避免序列化大对象，推荐传递ID
3. **异常分类**: 合理配置retryFor和noRetryFor
4. **监控告警**: 监控重试任务的执行情况
5. **资源清理**: 定期清理历史数据

## 注意事项

- 框架不对业务逻辑做任何假设，成功/失败判断完全由调用方决定
- 重试任务表与业务记录表分离，各司其职
- 支持分布式部署，多实例间通过数据库锁协调
- 异步执行重试任务，不阻塞主流程

## 许可证

MIT License
