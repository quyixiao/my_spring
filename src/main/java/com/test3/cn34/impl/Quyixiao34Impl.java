package com.test3.cn34.impl;

import com.test.LogUtils;
import com.test3.cn34.Person34;
import org.springframework.stereotype.Service;


@Service
public class Quyixiao34Impl implements Person34 {
    @Override
    public void perform() {
        LogUtils.info("to chang ge ");
    }
}
