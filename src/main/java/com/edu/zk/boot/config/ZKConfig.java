package com.edu.zk.boot.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZKConfig {

    private final ZKProps zkProps;

    @Autowired
    public ZKConfig(ZKProps zkProps){
        this.zkProps = zkProps;
    }

    @Bean
    public CuratorFramework curatorFramework () {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(zkProps.getTimeout(), zkProps.getRetry());
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(zkProps.getUrl(), retryPolicy);
        curatorFramework.start();
        return curatorFramework;
    }
}
