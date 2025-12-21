package com.example.retry.dao.impl;

import com.example.retry.dao.RetryTaskDao;
import com.example.retry.entity.RetryTask;
import com.example.retry.enums.BackoffStrategy;
import com.example.retry.enums.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 重试任务数据访问实现
 */
@Repository
public class RetryTaskDaoImpl implements RetryTaskDao {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<RetryTask> rowMapper = new RetryTaskRowMapper();
    
    @Override
    public int insert(RetryTask task) {
        String sql = """
            INSERT INTO retry_task (
                task_key, method_name, params_json, status, attempt_count,
                max_attempts, next_retry_time, deadline, max_retry_duration, backoff_strategy,
                created_at, updated_at, first_failed_at, last_error_msg, last_error_time,
                version, locked_by, locked_at, business_id, business_type
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        int result = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            int idx = 1;
            ps.setString(idx++, task.getTaskKey());
            ps.setString(idx++, task.getMethodName());
            ps.setString(idx++, task.getParamsJson());
            ps.setString(idx++, task.getStatus().name());
            ps.setInt(idx++, task.getAttemptCount());
            ps.setInt(idx++, task.getMaxAttempts());
            ps.setTimestamp(idx++, Timestamp.valueOf(task.getNextRetryTime()));
            ps.setTimestamp(idx++, task.getDeadline() != null ? Timestamp.valueOf(task.getDeadline()) : null);
            ps.setObject(idx++, task.getMaxRetryDuration());
            ps.setString(idx++, task.getBackoffStrategy().name());
            ps.setTimestamp(idx++, Timestamp.valueOf(task.getCreatedAt()));
            ps.setTimestamp(idx++, Timestamp.valueOf(task.getUpdatedAt()));
            ps.setTimestamp(idx++, task.getFirstFailedAt() != null ? Timestamp.valueOf(task.getFirstFailedAt()) : null);
            ps.setString(idx++, task.getLastErrorMsg());
            ps.setTimestamp(idx++, task.getLastErrorTime() != null ? Timestamp.valueOf(task.getLastErrorTime()) : null);
            ps.setInt(idx++, task.getVersion());
            ps.setString(idx++, task.getLockedBy());
            ps.setTimestamp(idx++, task.getLockedAt() != null ? Timestamp.valueOf(task.getLockedAt()) : null);
            ps.setString(idx++, task.getBusinessId());
            ps.setString(idx++, task.getBusinessType());
            return ps;
        }, keyHolder);
        
        if (result > 0 && keyHolder.getKey() != null) {
            task.setId(keyHolder.getKey().longValue());
        }
        
        return result;
    }
    
    @Override
    public RetryTask findByTaskKey(String taskKey) {
        String sql = "SELECT * FROM retry_task WHERE task_key = ?";
        List<RetryTask> tasks = jdbcTemplate.query(sql, rowMapper, taskKey);
        return tasks.isEmpty() ? null : tasks.get(0);
    }
    
    @Override
    public int update(RetryTask task) {
        String sql = """
            UPDATE retry_task SET
                status = ?, attempt_count = ?, next_retry_time = ?, 
                last_error_msg = ?, last_error_time = ?, updated_at = ?,
                version = version + 1, locked_by = ?, locked_at = ?
            WHERE id = ?
            """;
        
        return jdbcTemplate.update(sql,
            task.getStatus().name(),
            task.getAttemptCount(),
            Timestamp.valueOf(task.getNextRetryTime()),
            task.getLastErrorMsg(),
            task.getLastErrorTime() != null ? Timestamp.valueOf(task.getLastErrorTime()) : null,
            Timestamp.valueOf(LocalDateTime.now()),
            task.getLockedBy(),
            task.getLockedAt() != null ? Timestamp.valueOf(task.getLockedAt()) : null,
            task.getId()
        );
    }
    
    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM retry_task WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
    
    @Override
    public List<RetryTask> findPendingTasks(LocalDateTime beforeTime, int limit) {
        String sql = """
            SELECT * FROM retry_task 
            WHERE status = 'PENDING' AND next_retry_time <= ?
            ORDER BY next_retry_time ASC
            LIMIT ?
            """;
        
        return jdbcTemplate.query(sql, rowMapper, Timestamp.valueOf(beforeTime), limit);
    }
    
    @Override
    public int updateStatusWithVersion(Long id, TaskStatus fromStatus, TaskStatus toStatus, 
                                     Integer fromVersion, String lockedBy) {
        String sql = """
            UPDATE retry_task SET 
                status = ?, version = version + 1, locked_by = ?, locked_at = ?, updated_at = ?
            WHERE id = ? AND status = ? AND version = ?
            """;
        
        return jdbcTemplate.update(sql,
            toStatus.name(),
            lockedBy,
            Timestamp.valueOf(LocalDateTime.now()),
            Timestamp.valueOf(LocalDateTime.now()),
            id,
            fromStatus.name(),
            fromVersion
        );
    }
    
    @Override
    public int releaseTimeoutLocks(LocalDateTime timeoutBefore) {
        String sql = """
            UPDATE retry_task SET 
                status = 'PENDING', locked_by = NULL, locked_at = NULL, updated_at = ?
            WHERE status = 'RUNNING' AND locked_at < ?
            """;
        
        return jdbcTemplate.update(sql, 
            Timestamp.valueOf(LocalDateTime.now()),
            Timestamp.valueOf(timeoutBefore)
        );
    }
    
    /**
     * 行映射器
     */
    private static class RetryTaskRowMapper implements RowMapper<RetryTask> {
        @Override
        public RetryTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            RetryTask task = new RetryTask();
            
            task.setId(rs.getLong("id"));
            task.setTaskKey(rs.getString("task_key"));
            task.setMethodName(rs.getString("method_name"));
            task.setParamsJson(rs.getString("params_json"));
            
            task.setStatus(TaskStatus.valueOf(rs.getString("status")));
            task.setAttemptCount(rs.getInt("attempt_count"));
            
            task.setMaxAttempts(rs.getInt("max_attempts"));
            task.setNextRetryTime(rs.getTimestamp("next_retry_time").toLocalDateTime());
            
            Timestamp deadline = rs.getTimestamp("deadline");
            if (deadline != null) {
                task.setDeadline(deadline.toLocalDateTime());
            }
            
            task.setMaxRetryDuration(rs.getLong("max_retry_duration"));
            task.setBackoffStrategy(BackoffStrategy.valueOf(rs.getString("backoff_strategy")));
            
            task.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            task.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            
            Timestamp firstFailedAt = rs.getTimestamp("first_failed_at");
            if (firstFailedAt != null) {
                task.setFirstFailedAt(firstFailedAt.toLocalDateTime());
            }
            
            task.setLastErrorMsg(rs.getString("last_error_msg"));
            
            Timestamp lastErrorTime = rs.getTimestamp("last_error_time");
            if (lastErrorTime != null) {
                task.setLastErrorTime(lastErrorTime.toLocalDateTime());
            }
            
            task.setVersion(rs.getInt("version"));
            task.setLockedBy(rs.getString("locked_by"));
            
            Timestamp lockedAt = rs.getTimestamp("locked_at");
            if (lockedAt != null) {
                task.setLockedAt(lockedAt.toLocalDateTime());
            }
            
            task.setBusinessId(rs.getString("business_id"));
            task.setBusinessType(rs.getString("business_type"));
            
            return task;
        }
    }
}
