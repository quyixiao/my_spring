package com.test3.cn32;

import com.test.LogUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Repository;

@Aspect
@Repository
public class LogAspect32 {

    @Pointcut("execution(* com.test3.*.*.*(..)) && args(args)")
    public void method(String args) {

    }

    @Before("method(args)")
    public void interceptThoughts(String args) {

        LogUtils.info("interceptThoughts volunteer thoughts : " + args);
    }


}
