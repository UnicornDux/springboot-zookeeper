package com.edu.zk.boot.aop;

import com.edu.zk.boot.anno.LockKeyParam;
import com.edu.zk.boot.anno.ZooLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


@Slf4j
@Aspect
@Component
public class ZooLockAspect {
    private final CuratorFramework curatorFramework;
    private static final String KEY_PREFIX = "DISTRIBUTE_LOCK_";
    private static final String KEY_SEPARATOR = "/";

    @Autowired
    public ZooLockAspect(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    /**
     * 环绕切面
     */
    @Around("@annotation(zooLock)")
    public Object zooLock (ProceedingJoinPoint joinPoint, ZooLock zooLock) throws Throwable {
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        // 获取参数
        Object[] args = joinPoint.getArgs();

        if (!StringUtils.hasLength(zooLock.key())) {
           throw new RuntimeException("分布式锁键不能为空");
        }
        //
        String tname = Thread.currentThread().getName();
        String lockKey = buildLockKey(zooLock, method, args);
        InterProcessMutex lock = new InterProcessMutex(curatorFramework,lockKey);
        try {
            log.info("----{} 开始抢占锁 ----", tname);
            if (lock.acquire(zooLock.timeout(), zooLock.timeUnit())) {
                log.info("----{} 获取到锁 ---- ", tname);
                return joinPoint.proceed();
            } else {
                throw new RuntimeException("请勿重复提交");
            }
        }finally {
            log.info("----{} 释放锁 ----", tname);
            lock.release();
        }
    }

    /**
     * 构建分布式锁的 key
     * @param zooLock
     * @param method
     * @param args
     * @return
     */
    private String buildLockKey(
            ZooLock zooLock,
            Method method,
            Object[] args
    ) throws NoSuchFieldException, IllegalAccessException {

        StringBuilder key = new StringBuilder(KEY_SEPARATOR + KEY_PREFIX + zooLock.key());

        // 迭代全部参数的注解， 根据 LockKeyParam 的注解的参数所在的下标，来获取对应下标的参数值
        // 将这部分 key 拼接到 key 上
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                // 注解不是 @LockKeyParam
                if (!annotation.annotationType().isInstance(LockKeyParam.class)) {
                    continue;
                }
                // 获取 @LockKeyParam 中指定的所有字段
                String[] fields = ((LockKeyParam) annotation).fields();

                if (fields.length == 0) {
                    if (args[i] == null) {
                        throw new RuntimeException("动态参数不能为 null");
                    }
                    key.append(KEY_SEPARATOR).append(args[i]);
                }else {
                    // @LockKeyParam 的 field 值不为 null, 所以当前参数应该是对象类型
                    for (String field : fields) {
                        Class<?> clazz = args[i].getClass();
                        Field declaredField = clazz.getDeclaredField(field);
                        declaredField.setAccessible(true);
                        Object value = declaredField.get(clazz);
                        key.append(KEY_SEPARATOR).append(value);
                    }
                }
            }
        }
        return key.toString();
    }
}
