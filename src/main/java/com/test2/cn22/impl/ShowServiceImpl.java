package com.test2.cn22.impl;

import com.test.LogUtils;
import com.test2.cn22.ShowService;
import org.springframework.stereotype.Service;


@Service
public class ShowServiceImpl implements ShowService {
    @Override
    public void show(String param) {
        LogUtils.info(" show params :" + param);
    }



}
