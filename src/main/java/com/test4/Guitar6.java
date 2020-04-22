package com.test4;

import com.test.LogUtils;
import org.springframework.stereotype.Component;

@Component
public class Guitar6 implements Instrument6 {



    @Override
    public void play() {
        LogUtils.info("guitar 6666666666666666666");
    }
}
