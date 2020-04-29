package com.design.pattern.proxy.dynamic;

import com.design.pattern.proxy.statics.Person;

public class TestDy  {
    public static void main(String[] args) throws Exception{
        Person obj = (Person) new JDKMeipo().getInstance(new Customer());
        System.out.println("============================方法开始调用");
        obj.findLove();
    }
}
