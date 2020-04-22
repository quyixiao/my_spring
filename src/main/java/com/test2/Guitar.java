package com.test2;

import com.test.LogUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component("xxxxxxxxxxxx")
public class Guitar implements Instrument {
    @Override
    public void play() {
        LogUtils.info("111111111111111111111111");
    }
}
