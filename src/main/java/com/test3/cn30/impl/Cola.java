package com.test3.cn30.impl;

import com.test.LogUtils;
import com.test3.cn30.Liquid1;
import org.springframework.stereotype.Service;

@Service
public class Cola implements Liquid1 {
    @Override
    public void color() {
        LogUtils.info(" red ");
    }
}
