package com.edu.zk.master;

import lombok.extern.slf4j.Slf4j;
import java.util.stream.IntStream;

/**
 * 现实的开发场景中，有时候我们需要再一个集群中挑选一台来执行我们的任务，
 * 如果多个机器同时触发会造成数据一致性的问题. (例如定时任务)
 * ---------------------------------------------------------------
 *   > 这里我们的需求就变成了，在多个机器中选中一台来执行任务，
 *   > 确保只有一台会被选中,
 * ---------------------------------------------------------------
 *   > 我们利用  zookeeper 临时无序节点只有一个机器能创建成功的特性
 *     - 创建成功则去执行这个任务
 *     - 创建失败不执行
 */
@Slf4j
public class ElectorMaster {
    public static void main(String[] args) throws InterruptedException {

        // ------------------------------------------------------------
        // 启动线程方式 1.
        // ------------------------------------------------------------
        /*
        CountDownLatch latch = new CountDownLatch(4);
        Runnable r = () -> {
            latch.countDown();
            // 使用线程模拟机器
            Task task = new Task(Thread.currentThread().getName());
            task.go();
        };
        new Thread(r, "machine 1").start();
        new Thread(r, "machine 2").start();
        new Thread(r, "machine 3").start();
        new Thread(r, "machine 4").start();
        */


        // ------------------------------------------------------------
        // 启动线程方式 2
        // ------------------------------------------------------------
        IntStream.rangeClosed(1,5)
                .mapToObj( index -> "machine" + index)
                .map(Task::new)
                .map(task -> (Runnable) task::go)
                .map(Thread::new)
                .forEach(Thread::start);
        // 主程序阻塞等待
        Thread.sleep(20000);
    }
}
