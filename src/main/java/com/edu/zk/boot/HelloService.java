package com.edu.zk.boot;

import com.edu.zk.boot.anno.ZooLock;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    @ZooLock(key="hello")
    public String hello(String hello){
        return hello;
    }

}
