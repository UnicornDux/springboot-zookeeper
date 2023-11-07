package com.edu.zk.register;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        Stream.of("192.168.110.10", "192.168.110.11")
                .map(e -> (Runnable) () -> new UserService().registerService(e))
                .map(Thread::new)
                .forEach(Thread::start);

        TimeUnit.SECONDS.sleep(1);
        Stream.of("192.168.119.88", "192.168.119.99")
                .map(e -> (Runnable) () -> new OrderService().registerService(e))
                .map(Thread::new)
                .forEach(Thread::start);

        TimeUnit.MINUTES.sleep(10);
    }
}
