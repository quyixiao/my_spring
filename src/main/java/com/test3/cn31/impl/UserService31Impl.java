package com.test3.cn31.impl;

import com.test.LogUtils;
import com.test3.cn31.UserService31;
import org.springframework.stereotype.Service;


@Service
public class UserService31Impl implements UserService31 {
    @Override
    public void getName() {
        LogUtils.info(" getName ");
    }
}
