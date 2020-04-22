package com.test2.cn21;

import com.test.LogUtils;
import org.springframework.stereotype.Component;

import javax.inject.Named;


@Component
public class Man implements Person {
    @Override
    public void hair() {
        LogUtils.info("短");
    }
}
