package com.design.pattern.proxy.dynamic1;

public class ZhangSan implements Student {
    @Override
    public void changge(String name) {
        System.out.println("唱" + name);
    }

    @Override
    public int learn(Integer index) {
        System.out.println(" 学习第" + index + "课");
        return index;
    }


    @Override
    public String love(String name, Integer index) {
        System.out.println(" 爱" + name + " , " + index);
        return "quyixiao";
    }
}
