package com.test2.cn22.impl;

import com.test.LogUtils;
import com.test2.cn22.UserService;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {

    @Override
    public void selectByUserName() {

        LogUtils.info("select By username select ");
    }




}
