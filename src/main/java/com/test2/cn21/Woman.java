package com.test2.cn21;

import com.test.LogUtils;
import org.springframework.stereotype.Component;


@Component
@StringedInstrument2
public class Woman implements Person {
    @Override
    public void hair() {
        LogUtils.info("长");
    }
}
