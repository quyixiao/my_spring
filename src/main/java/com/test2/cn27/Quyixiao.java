package com.test2.cn27;

import com.test.LogUtils;
import org.springframework.stereotype.Service;

@Service
public class Quyixiao implements Peple {
    @Override
    public void getName() {
        LogUtils.info(" my name is quyixiao");
    }
}
