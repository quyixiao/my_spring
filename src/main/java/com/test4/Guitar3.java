package com.test4;

import com.test.LogUtils;
import com.test2.Instrument;
import com.test3.StringedInstrument;
import org.springframework.stereotype.Component;

@Component
@StringedInstrument
public class Guitar3 implements Instrument {
    @Override
    public void play() {
        LogUtils.info("Guitar3333333333333333333333333333");
    }
}
