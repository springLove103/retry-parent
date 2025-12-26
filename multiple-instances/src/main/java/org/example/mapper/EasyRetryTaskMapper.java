package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.entity.EasyRetryTask;

import java.util.List;

@Mapper
public interface EasyRetryTaskMapper  extends BaseMapper<EasyRetryTask> {


    /**
     * 查询待处理任务（分片）
     */
    @Select("SELECT * FROM easy_retry_task " +
            "WHERE lock_status = 1 " +
            "AND MOD(id, #{total}) = #{index} " +
            "LIMIT #{size}")
    List<EasyRetryTask> selectPendingTasks(@Param("total") Integer total,
                                           @Param("index") Integer index,
                                           @Param("size") Integer size);

    /**
     * 更新任务状态为处理中
     */
    @Update("UPDATE easy_retry_task " +
            "SET lock_status = 2 " +
            "WHERE id = #{id} AND lock_status = 1 ")
    int updateToProcessing(@Param("id") Long id);
}
