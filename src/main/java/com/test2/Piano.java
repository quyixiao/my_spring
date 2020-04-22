package com.test2;

import com.test.LogUtils;
import org.springframework.stereotype.Component;


@Component
public class Piano implements Instrument {
    @Override
    public void play() {
        LogUtils.info("PLINK PLINK PLINK ");
    }
}
