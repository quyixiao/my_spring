package com.test4;

import com.test.LogUtils;
import org.springframework.beans.factory.annotation.Qualifier;

@Qualifier
public class Guitar3 implements Instrument4 {
    @Override
    public void play() {
        LogUtils.info("Guitar3333333333333333333333333333");
    }
}
