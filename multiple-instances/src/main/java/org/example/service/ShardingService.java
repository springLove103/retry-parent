package org.example.service;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.example.dto.ShardingInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ShardingService {
    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    private NacosServiceManager nacosServiceManager;

    public ShardingInfo getShardingInfo() {
        // 获取 NamingService
        NamingService namingService = nacosServiceManager.getNamingService();
        // 1. 从Nacos获取所有健康实例
        List<Instance> instances = null;
        try {
            instances = namingService.selectInstances("multiple-service", true);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }

        // 2. 排序（保证每个实例看到的列表顺序一致）
        instances.sort(Comparator.comparing(i -> i.getIp() + ":" + i.getPort()));
        
        // 3. 计算 Total 和 Index
        int total = instances.size();
        int index = -1;
        String myIp = nacosDiscoveryProperties.getIp();
        int myPort = nacosDiscoveryProperties.getPort();
        
        for (int i = 0; i < instances.size(); i++) {
            if (instances.get(i).getIp().equals(myIp) && instances.get(i).getPort() == myPort) {
                index = i;
                break;
            }
        }
        return new ShardingInfo(index, total);
    }
}