package com.test3.cn35;


import com.test.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class AnnotationAspect {


    @Pointcut("execution(* com.test3.cn35.*.*(..))")
    public void aspect() {

    }


    @Before("aspect()")
    public void before(JoinPoint joinpoint) {
        log.info("before 通知");
    }


    @Before("aspect()")
    public void after(JoinPoint joinpoint) {
        log.info("after 通知");
    }


    @Around("aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object object = point.proceed();
        long endTime = System.currentTimeMillis();
        LogUtils.info("exe :" + (endTime - startTime));
        return object;
    }

    @AfterReturning("aspect()")
    public void afterReturning(JoinPoint joinPoint){
        log.info("返回通知："  + joinPoint);
    }

    @AfterThrowing("aspect()")
    public void afterThrowing(JoinPoint joinPoint){
        log.info("异常通知："  + joinPoint);
    }
}

