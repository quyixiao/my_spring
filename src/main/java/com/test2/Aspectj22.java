package com.test2;


import com.test.LogUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class Aspectj22 {


    public Object doAround(ProceedingJoinPoint pjp) {
        Object result = null;
        try {
            LogUtils.info("111111111111111111111111111111111");
            result = pjp.proceed();
            LogUtils.info("22222222222222222222222222222222");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return result;
    }


}
