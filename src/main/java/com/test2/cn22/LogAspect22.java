package com.test2.cn22;

import com.test.LogUtils;
import org.aspectj.lang.ProceedingJoinPoint;

public class LogAspect22 {


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
