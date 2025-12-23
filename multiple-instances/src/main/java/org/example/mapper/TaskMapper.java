package org.example.mapper;

@Mapper
public interface TaskMapper {

    /**
     * 根据分片逻辑查询任务
     */
    @Select("SELECT * FROM task_table " +
            "WHERE status = 'PENDING' " +
            "AND MOD(id, #{total}) = #{index} " +
            "LIMIT #{size}")
    List<TaskEntity> selectTasksBySharding(@Param("index") int index, 
                                           @Param("total") int total, 
                                           @Param("size") int size);

    /**
     * 乐观锁更新状态（双重保险）
     */
    @Update("UPDATE task_table SET status = 'PROCESSING', update_time = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int lockTask(@Param("id") Long id);
}