package com.test2.cn28.impl;

import com.test.LogUtils;
import com.test2.cn28.MindReader;
import org.springframework.stereotype.Service;

@Service
public class Magician implements MindReader {
    private String thoughts;

    @Override
    public void interceptThoughts(String thoughts) {
        LogUtils.info("interceptThoughts volunteer thoughts : " + thoughts);
        this.thoughts = thoughts;
    }

    @Override
    public String getThoughts() {
        return thoughts;
    }
}
