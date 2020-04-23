package com.test3.cn30.impl;

import com.test.LogUtils;
import com.test3.cn30.Liquid2;
import org.springframework.stereotype.Service;


@Service
public class Water implements Liquid2 {


    @Override
    public void drink() {
        LogUtils.info(" water to drink ");
    }


}
