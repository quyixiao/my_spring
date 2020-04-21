package com.test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Stage {
    private Stage() {
        log.info("创建Stage 方法");
    }

    private static class StageSingletonHolder {
        static Stage instance = new Stage();
    }

    public static Stage getInstance() {
        log.info("调用instance ");
        return StageSingletonHolder.instance;
    }

}
