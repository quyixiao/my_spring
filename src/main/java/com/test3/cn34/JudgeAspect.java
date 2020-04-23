package com.test3.cn34;


import com.test.LogUtils;
import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class JudgeAspect {
    public JudgeAspect() {


    }



    private CriticismEngine criticismEngineAspect;



    @Pointcut("execution( * com.test3.cn34.*.*(..))")
    public void perormance() {

    }



    @After("perormance()")
    public void after(){
        LogUtils.info("============"+ criticismEngineAspect.getCriticism());
    }


    @AfterReturning("perormance()")
    public void returning(){
        LogUtils.info("---------------------"+ criticismEngineAspect.getCriticism());
    }


    public CriticismEngine getCriticismEngineAspect() {
        return criticismEngineAspect;
    }



    public void setCriticismEngineAspect(CriticismEngine criticismEngineAspect) {
        this.criticismEngineAspect = criticismEngineAspect;
    }

}
