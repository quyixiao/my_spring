package com.test3.cn33;

import com.test3.cn33.impl.Contestant33Impl;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class ContestantIntroducer {


    @DeclareParents(value = "com.test3.cn33.UserService33+", defaultImpl = Contestant33Impl.class)
    public static Contestant33 contestant33;

}
