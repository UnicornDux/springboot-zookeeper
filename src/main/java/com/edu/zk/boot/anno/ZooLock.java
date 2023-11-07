package com.edu.zk.boot.anno;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ZooLock {
    /**
     * 分布式锁的 key
     */
    String key();

    /**
     * 锁释放时间，默认 5s
     */
    long timeout() default 5 * 1000;

    /**
     * 超时时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
