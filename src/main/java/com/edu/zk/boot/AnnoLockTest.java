package com.edu.zk.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class AnnoLockTest implements CommandLineRunner {

    @Autowired
    private HelloService helloService;

    @Override
    public void run(String... args) throws Exception {
        Runnable r = () -> {
            String ok = helloService.hello("ok");
            log.info("----{} 获取到结果 {}----", Thread.currentThread().getName(), ok);
        };

        Thread t1 = new Thread(r, "1");
        Thread t2 = new Thread(r, "2");
        Thread t3 = new Thread(r, "3");
        t1.start();
        t2.start();
        t3.start();

        Thread.sleep(10000);
    }
}
