package com.test2.cn28.impl;

import com.test2.cn28.Thinker;
import org.springframework.stereotype.Service;


@Service
public class Volunteer implements Thinker {



    private String thoughts;

    @Override
    public void thinkOfSomething(String thoughts) {
        this.thoughts = thoughts;
    }

    public String getThoughts() {
        return thoughts;
    }
}
