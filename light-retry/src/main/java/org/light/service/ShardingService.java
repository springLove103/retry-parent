package org.light.service;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.light.dto.ShardingInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ShardingService {
    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    private NacosServiceManager nacosServiceManager;


    /*一个小建议： 由于 Nacos 的实例列表可能会因为网络抖动短暂变化，
    建议在执行 SQL 捞取数据前，先判断 index 是否合法（>-1）。如果获取失败，该次定时任务可以跳过，等待下次触发，以防数据分配出错。*/
    public ShardingInfo getShardingInfo(String serviceName) {
        try {
            NamingService namingService = nacosServiceManager.getNamingService();
            // 1. 获取所有健康实例
            List<Instance> instances = namingService.selectInstances(serviceName, true);

            // 2. 排序：这是最关键的一步，必须保证所有节点拿到的顺序完全一致
            instances.sort(Comparator.comparing(i -> i.getIp() + ":" + i.getPort()));

            int total = instances.size();
            int index = -1;

            // 获取当前实例的 IP 和 端口
            String myIp = nacosDiscoveryProperties.getIp();
            int myPort = nacosDiscoveryProperties.getPort();

            // 3. 匹配当前实例位置
            for (int i = 0; i < total; i++) {
                Instance instance = instances.get(i);
                if (instance.getIp().equals(myIp) && instance.getPort() == myPort) {
                    index = i;
                    break;
                }
            }

            if (index == -1) {
                log.warn("当前实例未在Nacos健康列表中匹配到，可能是心跳延迟。Ip:{}, Port:{}", myIp, myPort);
            }

            return new ShardingInfo(index, total);
        } catch (NacosException e) {
            log.error("从Nacos获取实例列表失败", e);
            return new ShardingInfo(-1, 0);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            List<Integer> shards = calculateShards(i, 3, 64);
            System.out.println(shards);
        }
//        List<Integer> shards = calculateShards(0, 3, 64);
//        System.out.println(shards);
    }

    public static List<Integer> calculateShards(int myIndex, int totalInstances, int totalShards) {
        if (myIndex < 0 || myIndex >= totalInstances) {
            throw new IllegalArgumentException("myIndex out of range");
        }
        List<Integer> result = new ArrayList<>();

        // 假设 totalShards=64, totalInstances=10
        // 平均每人分几个？ 64 / 10 = 6
        int avg = totalShards / totalInstances;

        // 还剩下几个分不完？ 64 % 10 = 4 (余数)
        // 这 4 个片，通常分给排名前 4 的人 (index 0~3)
        int remainder = totalShards % totalInstances;
        // 1. 确定起点 (start)
        // 前 myIndex 个人，有些人分到了 avg+1 个，有些人分到了 avg 个
        // 简单的公式推导：
        int start = 0;
        for (int i = 0; i < myIndex; i++) {
            // 如果 index < remainder，说明这个人分到了 avg+1 个
            start += avg + (i < remainder ? 1 : 0);
        }
        // 2. 确定我应该领几个 (count)
        // 如果我的排名在余数范围内 (myIndex < 4)，我就领 avg+1 (7个)
        // 否则我就领 avg (6个)
        int myCount = avg + (myIndex < remainder ? 1 : 0);
        // 3. 生成 ID 列表
        for (int i = 0; i < myCount; i++) {
            result.add(start + i);
        }

        return result;
    }
}