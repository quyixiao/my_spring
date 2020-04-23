package com.test3.cn32.impl;

import com.test.LogUtils;
import com.test3.cn32.Thinker32;
import org.springframework.stereotype.Service;


@Service
public class Volunteer32 implements Thinker32 {


    private String thoughts;

    @Override
    public void thinkOfSomething(String thoughts) {
        LogUtils.info("thinkOfSomething :" + thoughts);
        this.thoughts = thoughts;
    }

    public String getThoughts() {
        return thoughts;
    }
}
