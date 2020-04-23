package com.test3.cn31;


import com.test.LogUtils;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class Audience31 {

    @Pointcut("execution(* com.test3..*.*Impl.*(..)) ")
    public void performance() {

    }

    @Before("performance()")
    public void before1() {
        LogUtils.info("before1==========================================");
    }

    @Before("performance()")
    public void before2() {
        LogUtils.info("before2=================================");
    }


    @AfterReturning("performance()")
    public void afterRunning() {
        LogUtils.info("afterRunning");
    }


    @AfterThrowing("performance()")
    public void afterReturning() {
        LogUtils.info("afterThrowing");
    }


}

