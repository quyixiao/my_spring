package com.design.pattern.prototype;

import lombok.Data;

import java.util.List;

@Data
public class ConcreatePrototypeA implements Prototype {
    private int age;
    private String name ;
    private List hobbies;


    @Override
    public Prototype clone() {
        ConcreatePrototypeA concreatePrototypeA = new ConcreatePrototypeA();
        concreatePrototypeA.setAge(this.age);
        concreatePrototypeA.setName(this.name);
        concreatePrototypeA.setHobbies(this.hobbies);
        return concreatePrototypeA;
    }



}
