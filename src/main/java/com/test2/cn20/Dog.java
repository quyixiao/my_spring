package com.test2.cn20;

import com.test.LogUtils;
import com.test2.cn20.Animal;

import javax.inject.Named;

@Named("dog")
public class Dog implements Animal {
    @Override
    public void eat() {
        LogUtils.info(" eat dog 饭");
    }
}
