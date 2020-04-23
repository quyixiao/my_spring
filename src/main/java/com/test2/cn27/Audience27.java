package com.test2.cn27;

import com.test.LogUtils;
import org.aspectj.lang.ProceedingJoinPoint;

public class Audience27 {


    public void watchPerformance(ProceedingJoinPoint joinPoint) {
        try {
            LogUtils.info("watchPerformance start ");


            joinPoint.proceed();

            LogUtils.info("watchPerformance end");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
