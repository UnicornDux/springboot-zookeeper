package com.edu.zk.register;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class UserService {
    private static final String userServicePrefix = "/service/user";
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
            Stat stat = zk.exists(userServicePrefix, false);
            if (stat == null) {
                // 创建服务的持久化根节点
                zk.create(userServicePrefix, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
            userServicePrefix + "/" + ip,
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
                        try {
                            List<String> orderServiceIPs = autoFindOrderService(zk, ip);
                            if (orderServiceIPs.size() < 1) {
                                log.info("首次没有发现 orderService 服务节点");
                            } else {
                                orderServiceIPs.forEach(order -> {
                                    log.info("首次发现 orderService 服务节点: {}", order);
                                });
                            }
                        } catch (InterruptedException | KeeperException e) {
                            e.printStackTrace();
                            log.error("{} :: 获取 orderService 列表异常", this.getClass().getName());
                        }
                    }else {
                        // 目前只关注节点创建是否成功事件
                        log.info("监听到其他事件");
                    }
                }

                // 由于当前的用户服务调用了订单服务，因此需要监听订单服务
                // 一旦订单服务发生节点变化，能立即感知
                private List<String> autoFindOrderService(ZooKeeper zk, String ip) throws InterruptedException, KeeperException {
                    return zk.getChildren(orderServicePrefix, (event -> {
                        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            //一旦调用服务的节点发生变化则需要获取最新的子节点并继续监听订单服务节点
                            try {
                                List<String> orderServiceIPs = autoFindOrderService(zk, ip);
                                if (orderServiceIPs.size() < 1) {
                                    log.info("{}: {} 没有发现 orderService 服务节点", this.getClass().getName(), ip);
                                } else {
                                    orderServiceIPs.forEach(order -> {
                                        log.info("{}: {} 发现 orderService 服务节点: {}", this.getClass().getName(), ip, order);
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                log.error("{} :: 获取 orderService 列表异常", this.getClass().getName());
                            }
                        }else {
                            // 目前只关注子节点变化的事件
                            log.info(
                                    "监听到其他事件: {},{}",
                                    event.getType().getIntValue(),
                                    event.getState().getIntValue()
                            );
                        }
                    }));
                }
            }, "callback data"
        );
    }
}
