package com.test3.cn33.impl;

import com.test.LogUtils;
import com.test3.cn33.UserService33;
import org.springframework.stereotype.Service;

@Service
public class UserService33Impl implements UserService33 {
    @Override
    public void getName() {
        LogUtils.info(" quyixiao ");
    }
}
