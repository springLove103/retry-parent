package com.alibaba.easyretry.extension.mybatis.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class RetryTaskQuery {

    private Long lastId;

    private List<Integer> retryStatus;

    private String sharding;

    /**
     * 新增 limit 字段（默认 500），以防止 MyBatis 访问时找不到 getter
     */
    private Integer limit = 500;
}