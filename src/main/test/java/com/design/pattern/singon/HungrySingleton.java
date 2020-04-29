package com.design.pattern.singon;

import java.lang.reflect.Constructor;

public class HungrySingleton {

    private static final HungrySingleton HUNGRY_SINGLETON = new HungrySingleton();

    private HungrySingleton() {
        if(HUNGRY_SINGLETON !=null){
            throw new RuntimeException("不能创建多个单个");
        }
    }

    public static HungrySingleton getInstance() {
        return HUNGRY_SINGLETON;
    }


    public static void main(String[] args) {

        HungrySingleton hungrySingleton1 = HungrySingleton.getInstance();
        HungrySingleton hungrySingleton2 = HungrySingleton.getInstance();
        System.out.println(hungrySingleton1 == hungrySingleton2);

       /* try {
            Class<?> clazz = HungrySingleton.class;
            Constructor c = clazz.getDeclaredConstructor(null);
            //
            c.setAccessible(true);
            Object o1 = c.newInstance();
            Object o2 = c.newInstance();
            System.out.println(o1 == o2);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }
}
