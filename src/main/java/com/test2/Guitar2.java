package com.test2;

import com.test.LogUtils;
import com.test3.StringedInstrument;
import org.springframework.stereotype.Component;


@Component
@StringedInstrument
public class Guitar2 implements Instrument {
    @Override
    public void play() {
        LogUtils.info("22222222222222222");
    }
}
