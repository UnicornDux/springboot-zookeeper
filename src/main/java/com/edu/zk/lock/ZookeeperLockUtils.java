package com.edu.zk.lock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ZookeeperLockUtils {


    private static String address = "192.168.64.170:2181";
    public static CuratorFramework client;
    private static InterProcessMutex interProcessMutex;
    //使用static代码块实现单例
    static{
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.newClient(address, retryPolicy);
        client.start();
        interProcessMutex=new InterProcessMutex(client, "/curator/lock");
    }
    //私有构造 保证外界无法直接实例化
    private ZookeeperLockUtils(){

    }
    public static InterProcessMutex getMutex(){
        return interProcessMutex;
    }
    //获得了锁
    public static boolean acquire(long time, TimeUnit unit){
        try {
            return getMutex().acquire(time,unit);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    //释放锁
    public static void release(){
        try {
            getMutex().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            Runnable task = ZookeeperLockUtils::getInfo;
            executorService.execute(task);
        }
    }

    public static void getInfo(){
        boolean flag = false;
        try {
            //尝试获取锁，最多等待5秒
            flag = ZookeeperLockUtils.acquire(5, TimeUnit.SECONDS);
            Thread currentThread = Thread.currentThread();
            if(flag){
                System.out.println("线程"+currentThread.getId()+"获取锁成功");
            }else{
                System.out.println("线程"+currentThread.getId()+"获取锁失败");
            }
            System.out.println("业务逻辑");
        } finally{
            if(flag){
                try {
                    ZookeeperLockUtils.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
