package com.edu.zk.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class ZookeeperClient implements CommandLineRunner {

    public static final String CONNECT_ADDR = "192.168.64.170:2181";

    public static void main(String[] args) throws InterruptedException {

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 4);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(CONNECT_ADDR, retryPolicy);
        curatorFramework.start();
        InterProcessMutex lock = new InterProcessMutex( curatorFramework, "/locks/my-lock");
        CountDownLatch latch = new CountDownLatch(3);
        Runnable r = () -> {
           latch.countDown();
           String name = Thread.currentThread().getName();
           try {
               log.info("----->> {} :: 开始抢占锁<<---", name);
               if (lock.acquire(5000, TimeUnit.MILLISECONDS)){
                   log.info("----->> {} :: 抢占锁成功<<---", name);
                   Thread.sleep(100);
                   log.info("----->> {} :: 执行完成业务<<---", name);
               }
           } catch (Exception e) {
               throw new RuntimeException(e);
           }finally {
               log.info("----->> {} :: 释放锁<<---", name);
               try {
                   lock.release();
               } catch (Exception e) {
                   e.printStackTrace();
               }
           }
        };

        Thread t1 = new Thread(r, "1");
        Thread t2 = new Thread(r, "2");
        Thread t3 = new Thread(r, "3");
        t1.start();
        t2.start();
        t3.start();

        Thread.sleep(10000);
    }

    @Override
    public void run(String... args) throws Exception {
        main(args);
    }
}
