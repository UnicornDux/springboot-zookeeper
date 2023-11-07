package com.edu.zk.master;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Task {

    private static String ADDR = "192.168.64.170:2181";
    private static String path = "/task-path/task";
    private final String name;

    public Task(String name){
        this.name = name;
    }
    public void go() {
        // 由于zk连接是一个异步过程，后续操作应该等待连接完成之后才能操作
        // 使用信号量控制异步程序的阻塞，
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            ZooKeeper zk = new ZooKeeper(ADDR, 5000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    // 连接成功
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
            log.info("连接成功 --> {} ", zk);
            // 尝试创建根节点
            Stat exists = zk.exists("/task-path", false);
            if (exists == null) {
                zk.create("/task-path", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
            }
            tryToBeMaster(zk, this.name);
        } catch (IOException | InterruptedException | KeeperException e) {
            throw new RuntimeException(e);
        }

    }

    public void tryToBeMaster(ZooKeeper zk, String machine)  {
        // 指定异步创建，根据回调监控是否成功
        zk.create(
            path,  // 创建的路径
            "".getBytes(), // 在路径中写入的内容
            ZooDefs.Ids.OPEN_ACL_UNSAFE, // 节点的 ACL 列表
            CreateMode.EPHEMERAL, // 节点为临时节点
            (rc, path, ctx, name) -> {
                // 这里在暂时用不到 ctx 的参数，仅打印
                log.info("ctx :: {}", ctx);
                if (rc == KeeperException.Code.OK.intValue()) {
                    // 这里使用多线程模拟多台机器同时创建节点，所以打印线程名称，方便观察
                    log.info("{} -->> 创建主节点成功, 执行任务", machine);
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        zk.delete(path, -1);
                        // 这里模拟宕机，真实情况下，节点宕机，zk 会自动删除创建的临时节点
                        log.info("{} :: 宕机了", machine);
                    } catch (InterruptedException | KeeperException e) {
                        throw new RuntimeException(e);
                    }
                    // 节点已经存在
                }else if (rc == KeeperException.Code.NODEEXISTS.intValue()) {
                    log.info("{} :: 创建节点失败, 进入等待", machine);
                    try {
                        // 对目标文件夹添加监听器,
                        zk.exists(path, event -> {
                            if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                                tryToBeMaster(zk, machine);
                            }
                        });
                    } catch (InterruptedException | KeeperException e) {
                        throw new RuntimeException(e);
                    }
                }else {
                    // 其他状态我们不关注，不处理
                }
            }, "ctx_data" // 传入的上下文数据，可以作为内部异步回调的入参
        );
    }
}
