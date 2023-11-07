package com.edu.zk.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 关于 ZK 的配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "zk")
public class ZKProps {

    /**
     * zk 连接地址
     */
    private String url;


    /**
     * 超时时间(毫秒为单位，默认值1000)
     */
    private int timeout = 1000;

    /**
     * 重试次数
     */
    private int retry;

}
