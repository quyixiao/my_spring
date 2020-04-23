package com.test3.cn32.impl;

import com.test.LogUtils;
import com.test3.cn32.Person32;
import com.test3.cn32.Woman32;
import org.springframework.stereotype.Service;

@Service
public class Man32 implements Person32 {

    @Override
    public void name(String name) {
        LogUtils.info(" my name is " + name);
    }

    @Override
    public void setAge(int age) {
        LogUtils.info(" my age is " + age);
    }

    @Override
    public void setWife(Woman32 woman32) {

        LogUtils.info(" my woman is " + woman32);
    }
}
