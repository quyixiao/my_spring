package com.test;

public class Sonnet29 implements Poem {

    private static String[] lines = {
            "quyixiao",
            "xiazhaqnghong"
    };


    public Sonnet29() {
    }

    @Override
    public void recite() {
        for (int i = 0; i < lines.length; i++) {
            LogUtils.info(lines[i]);
        }
    }
}
