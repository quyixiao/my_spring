package com.test2;

import com.test.LogUtils;

public class Piano implements Instrument {
    @Override
    public void play() {
        LogUtils.info("PLINK PLINK PLINK ");
    }
}
