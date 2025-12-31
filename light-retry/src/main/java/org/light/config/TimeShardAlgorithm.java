package org.light.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.light.constant.GlobalConstant;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TimeShardAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = shardingValue.getColumnNameAndRangeValuesMap();

        List<String> physicTable = Lists.newArrayList();
        if (MapUtils.isNotEmpty(columnNameAndShardingValuesMap)) {
            Collection<Comparable<?>> shardCol = columnNameAndShardingValuesMap.get("sharding");
            Collection<Comparable<?>> gmtCol   = columnNameAndShardingValuesMap.get("gmt_create");
            if (CollectionUtils.isNotEmpty(shardCol) && CollectionUtils.isNotEmpty(gmtCol)) {
                Comparable<?> shardObj = shardCol.iterator().next();
                Comparable<?> gmtObj   = gmtCol.iterator().next();

                String shardStr = shardObj.toString();
                LocalDateTime gmt = convertToLocalDateTime(gmtObj);
                String month = gmt.format(DateTimeFormatter.ofPattern("yyyyMM"));
                int hashIdx = Math.abs(shardStr.hashCode() % GlobalConstant.M);
                physicTable.add("easy_retry_task_" + month + "_" + hashIdx);
            }
        }
        if (MapUtils.isNotEmpty(columnNameAndRangeValuesMap)) {
            Range<Comparable<?>> shardCol = columnNameAndRangeValuesMap.get("sharding");
            Range<Comparable<?>> gmtRange = columnNameAndRangeValuesMap.get("gmt_create");
            if (gmtRange != null) {
                LocalDateTime start = convertToLocalDateTime(gmtRange.lowerEndpoint());
                LocalDateTime end   = convertToLocalDateTime(gmtRange.upperEndpoint());
                // 按月展开所有可能后缀，再拼 hash 位
                for (LocalDateTime cur = start; !cur.isAfter(end); cur = cur.plusMonths(1)) {
                    String month = cur.format(DateTimeFormatter.ofPattern("yyyyMM"));
                    for (int i = 0; i < GlobalConstant.M; i++) {
                        physicTable.add("easy_retry_task_" + month + "_" + i);
                    }
                }
            }
        }
        return  physicTable;
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties props) {

    }

    private static LocalDateTime convertToLocalDateTime(Comparable<?> val) {
        if (val == null) {
            throw new IllegalArgumentException("时间值为 null");
        }

        // 1. 本来就是 LocalDateTime
        if (val instanceof LocalDateTime) {
            return (LocalDateTime) val;
        }

        // 2. 老项目常用 java.sql.Timestamp
        if (val instanceof Timestamp) {
            return ((Timestamp) val).toLocalDateTime();
        }

        // 3. java.util.Date
        if (val instanceof Date) {
            return LocalDateTime.ofInstant(((Date) val).toInstant(), ZoneId.systemDefault());
        }

        // 4. 时间戳（毫秒）
        if (val instanceof Long || val instanceof Integer) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) val).longValue()),
                    ZoneId.systemDefault());
        }

        // 5. 字符串（兜底，按需再扩展）
        if (val instanceof String) {
            return LocalDateTime.parse((String) val, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        // 都匹配不到就抛异常，避免隐式错误
        throw new IllegalArgumentException("Unsupported time type: " + val.getClass());
    }
}
