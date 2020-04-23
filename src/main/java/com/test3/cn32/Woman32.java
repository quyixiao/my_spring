package com.test3.cn32;

import lombok.Data;

@Data
public class Woman32 {

    private String name;
    private int age ;


    public Woman32() {
        this.name = name;
        this.age = age;
    }
    public Woman32(String name, int age) {
        this.name = name;
        this.age = age;
    }



    @Override
    public String toString() {
        return "Woman32{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
