package com.test3.cn34.impl;

import com.test3.cn34.CriticismEngine;
import org.springframework.stereotype.Service;


@Service
public class CriticismEngineImpl implements CriticismEngine {

    private String[] criticismPool;

    public CriticismEngineImpl() {

    }

    public String getCriticism() {
        int i = (int) (Math.random() * criticismPool.length);
        return criticismPool[i];
    }

    public void setCriticismPool(String[] criticismPool) {
        this.criticismPool = criticismPool;
    }
}
