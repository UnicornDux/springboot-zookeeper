package com.edu.zk.boot.anno;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LockKeyParam {

    /**
     * 由于注解生成的分布式锁的 key 可能需要根据用户入参动态变化，
     * -------------------------------------------------------------------
     * <p> 1) 如果用户传参是对象类型，那么需要指定哪些字段用于分布式锁 key 构建
     * <p> 2) 如果是基本类型则不需要这个注解
     * -------------------------------------------------------------------
     * <p>例1：public void count(@LockKeyParam({"id"}) User user)
     * <p>例2：public void count(@LockKeyParam({"id","userName"}) User user)
     * <p>例3：public void count(@LockKeyParam String userId)
     */
    String[] fields() default {};

}

