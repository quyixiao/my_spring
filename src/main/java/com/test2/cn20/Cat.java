package com.test2.cn20;

import com.test.LogUtils;
import com.test2.cn20.Animal;

import javax.inject.Named;

@Named("cat")
public class Cat implements Animal {
    @Override
    public void eat() {
        LogUtils.info(" finsh ");
    }
}
