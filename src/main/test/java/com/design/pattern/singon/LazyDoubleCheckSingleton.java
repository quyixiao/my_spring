package com.design.pattern.singon;

import java.lang.reflect.Constructor;

public class LazyDoubleCheckSingleton {


    private volatile static LazyDoubleCheckSingleton lazy = null;

    private LazyDoubleCheckSingleton() {

    }

    public static LazyDoubleCheckSingleton getInstance() {
        if (lazy == null) {
            synchronized (LazyDoubleCheckSingleton.class) {
                if (lazy == null) {
                    lazy = new LazyDoubleCheckSingleton();
                }
            }
        }
        return lazy;
    }


    public static void main(String[] args) {
        LazyDoubleCheckSingleton singleton1 = LazyDoubleCheckSingleton.getInstance();
        LazyDoubleCheckSingleton singleton2 = LazyDoubleCheckSingleton.getInstance();
        System.out.println(singleton1 == singleton2);


        try {
            Class<?> clazz = LazyDoubleCheckSingleton.class;
            Constructor c = clazz.getDeclaredConstructor(null);
            //
            c.setAccessible(true);
            Object o1 = c.newInstance();
            Object o2 = c.newInstance();
            System.out.println(o1 == o2);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
