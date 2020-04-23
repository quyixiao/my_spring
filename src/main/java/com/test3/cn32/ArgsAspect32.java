package com.test3.cn32;

import com.alibaba.fastjson.JSON;
import com.test.LogUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Repository;

@Aspect
@Repository
public class ArgsAspect32 {

    @Pointcut("execution(* com.test3.*.*.*(..)) && args(..)")
    public void method() {

    }


    @Before("method()")
    public void interceptThoughts(JoinPoint jp) {
        LogUtils.info("ArgsAspect32 interceptThoughts volunteer thoughts : " + JSON.toJSONString(jp.getArgs()));
    }


}
