package com.test2;

import com.test.LogUtils;

public class Saxophone implements Instrument {

    public Saxophone(){

    }

    @Override
    public void play() {
        LogUtils.info("TOOT TOOT TOOT ");
    }
}
