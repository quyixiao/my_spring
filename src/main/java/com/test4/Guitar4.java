package com.test4;

import com.test.LogUtils;
import com.test3.StringedInstrument;

@StringedInstrument
public class Guitar4 implements Instrument4 {
    @Override
    public void play() {
        LogUtils.info("Guitar444444444444444444444444");
    }
}
