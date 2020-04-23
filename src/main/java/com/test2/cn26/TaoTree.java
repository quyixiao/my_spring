package com.test2.cn26;

import com.test.LogUtils;
import org.springframework.stereotype.Service;

@Service
public class TaoTree implements Tree {
    @Override
    public void raise() {
        LogUtils.info(" 桃树 tree ");
    }
}
