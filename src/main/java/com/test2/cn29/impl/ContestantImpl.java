package com.test2.cn29.impl;

import com.test.LogUtils;
import com.test2.cn29.Contestant;

public class ContestantImpl implements Contestant {
    @Override
    public void receiveAward() {
        LogUtils.info("receiveAward");
    }
}
