package org.light.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.light.constant.GlobalConstant;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TimeShardAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = shardingValue.getColumnNameAndRangeValuesMap();

        Set<String> physicTable = Sets.newConcurrentHashSet();
        if (MapUtils.isNotEmpty(columnNameAndShardingValuesMap)) {
            Collection<Comparable<?>> shardCol = columnNameAndShardingValuesMap.get("sharding");
            Collection<Comparable<?>> gmtCol   = columnNameAndShardingValuesMap.get("gmt_create");
            if (CollectionUtils.isNotEmpty(shardCol) && CollectionUtils.isNotEmpty(gmtCol)) {
                Comparable<?> gmtObj   = gmtCol.iterator().next();
                LocalDateTime gmt = convertToLocalDateTime(gmtObj);
                String month = gmt.format(DateTimeFormatter.ofPattern("yyyyMM"));
                shardCol.forEach(item -> {
                    String shardStr = item.toString();
                    int hashIdx = Math.abs(shardStr.hashCode() % GlobalConstant.M);
                    physicTable.add("easy_retry_task_" + month + "_" + hashIdx);
                });
            }
        }
        if (MapUtils.isNotEmpty(columnNameAndRangeValuesMap)) {
//            Range<Comparable<?>> shardCol = columnNameAndRangeValuesMap.get("sharding");
            Collection<Comparable<?>> shardCol = columnNameAndShardingValuesMap.get("sharding");
            Range<Comparable<?>> gmtRange = columnNameAndRangeValuesMap.get("gmt_create");
            LocalDateTime start = convertToLocalDateTime(gmtRange.lowerEndpoint());
            LocalDateTime end   = convertToLocalDateTime(gmtRange.upperEndpoint());
            Set<String> allMonth = getAllMonth(start, end);
            shardCol.forEach(item -> {
                String shardStr = item.toString();
                int hashIdx = Math.abs(shardStr.hashCode() % GlobalConstant.M);
                allMonth.forEach(month -> {
                    physicTable.add("easy_retry_task_" + month + "_" + hashIdx);
                });
            });
        }
        return  physicTable;
    }

    private Set<String> getAllMonth(LocalDateTime start, LocalDateTime end) {
        YearMonth startYm = YearMonth.from(start);
        YearMonth endYm   = YearMonth.from(end);
        Set<String> months = Sets.newConcurrentHashSet();
        for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
            String month = ym.format(DateTimeFormatter.ofPattern("yyyyMM"));
            months.add(month);
        }
        return months;
    }

    public static void main(String[] args) {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end   = LocalDateTime.now();

        YearMonth startYm = YearMonth.from(start);
        YearMonth endYm   = YearMonth.from(end);
        for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
            String month = ym.format(DateTimeFormatter.ofPattern("yyyyMM"));
            System.out.println(month);
        }
//        for (LocalDateTime cur = start; !cur.isAfter(end); cur = cur.plusMonths(1)) {
//            String month = cur.format(DateTimeFormatter.ofPattern("yyyyMM"));
//            System.out.println(month);
//        }
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
