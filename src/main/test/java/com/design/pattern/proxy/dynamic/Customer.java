package com.design.pattern.proxy.dynamic;

import com.design.pattern.proxy.statics.Person;

public class Customer implements Person {
    @Override
    public void findLove() {
        System.out.println(" 高富帅 180米 ");
    }
}
