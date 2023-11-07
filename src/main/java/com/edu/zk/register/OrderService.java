package com.edu.zk.register;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OrderService {
    private static final String orderServicePrefix = "/service/order";
    private static final String ADDR = "192.168.64.170:2181";

    // 连接 Zookeeper
    // 注册到服务注册中心
    public void registerService(String ip) {
        try {
            // 连接客户端, 由于是异步操作，因此需要阻塞等待
            CountDownLatch latch = new CountDownLatch(1);
            ZooKeeper zk = new ZooKeeper(ADDR, 5000, (event) -> {
               latch.countDown();
            });
            latch.await();
            log.info("{} :: 连接客户端成功", this.getClass().getName());
            // 判断当前服务的根节点
            Stat stat = zk.exists(orderServicePrefix, false);
            if (stat == null) {
                // 创建服务的持久化根节点
                zk.create(orderServicePrefix, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            // 开始注册自己的服务到注册中心
            startRegister(zk, ip);
        } catch (IOException | InterruptedException | KeeperException e) {
            throw new RuntimeException(e);
        }
    }

    public void startRegister(ZooKeeper zk, String ip) {
        /**
         * 先尝试创建当前服务的临时顺序节点
         * ----------------------------------------
         * > 创建成功，表示服务注册成功
         * > 创建失败，直接退出，去查 IP 被占用的原因
         */
        zk.create(
            orderServicePrefix + "/" + ip,
            "".getBytes(),
            ZooDefs.Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL,
            new AsyncCallback.StringCallback() {
                @Override
                public void processResult(int rc, String path, Object ctx, String name) {
                    if (rc == KeeperException.Code.OK.intValue()) {
                        log.info(
                                "{}:{} 上线成功, Zookeeper 节点路径是 : {}",
                                this.getClass().getName(),
                                ip,
                                name
                        );
                        // 上线之后马上下线，看用户服务是否感知到服务的变化
                        try {
                            TimeUnit.SECONDS.sleep(1);
                            zk.delete(orderServicePrefix + "/" + ip, -1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        // 目前只关注节点创建是否成功事件
                        log.info("监听到其他事件");
                    }
                }
            }, "callback-data"
        );
    }
}
