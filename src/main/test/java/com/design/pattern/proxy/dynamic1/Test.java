package com.design.pattern.proxy.dynamic1;

import com.design.pattern.proxy.dynamic.Customer;
import com.design.pattern.proxy.statics.Person;

public class Test {


    public static void main(String[] args) throws Exception {
     /*   Person object = (Person) new GPMeipo().getInstance(new Customer());
        System.out.println(object.getClass());
        object.findLove();*/

        Student object = (Student) new GPMeipo().getInstance(new ZhangSan());
        object.changge("烟花易冷");
        object.learn(10);
        object.love("小敏",0);

    }
}
