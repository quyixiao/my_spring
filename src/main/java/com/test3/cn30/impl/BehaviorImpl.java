package com.test3.cn30.impl;

import com.test.LogUtils;
import com.test3.cn30.Behavior;
import org.springframework.stereotype.Service;


@Service
public class BehaviorImpl implements Behavior {
    @Override
    public void flow() {
        LogUtils.info("liquid  remove ");
    }
}
