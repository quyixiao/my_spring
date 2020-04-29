package com.design.pattern.singon;

public class LazyInnerClassSingleton {
    //如果没有使用，内部类是不能加载的
    private LazyInnerClassSingleton() {
        System.out.println("初始化xxxxxx");
        if (LazyHolder.LAZY != null) {
            throw new RuntimeException("不能创建多个实例");
        }
    }

    // 第一个关键字都不是多余的，static 是为了单例空间共享，保证这个方法不会被重写，重载
    public static final LazyInnerClassSingleton getInstance() {
        // 在返回结果之前，一定会先加载内部类
        System.out.println("调用xxxxxxxxxxxxxxxxxxxxxx");
        return LazyHolder.LAZY;
    }

    // 默认是不加载的
    private static class LazyHolder {
        private static final LazyInnerClassSingleton LAZY = new LazyInnerClassSingleton();
    }
}
