package com.design.pattern.prototype;

import java.util.ArrayList;
import java.util.List;

public class PrototypeTest {


    public static void main(String[] args) {
        // 创建一个要具体克隆的对象
        ConcreatePrototypeA concreatePrototypeA = new ConcreatePrototypeA();
        // 填充属性
        concreatePrototypeA.setAge(18);
        concreatePrototypeA.setName("quyixiao");
        List hibbies = new ArrayList();
        concreatePrototypeA.setHobbies(hibbies);
        System.out.println(concreatePrototypeA);


        // 创建一个Client 对象 ，准备克隆
        Client client = new Client(concreatePrototypeA);
        ConcreatePrototypeA concreatePrototypeA1 = (ConcreatePrototypeA) client.startClone(concreatePrototypeA);
        System.out.println(concreatePrototypeA1);

        System.out.println("克隆对象的引用类型地址：" + concreatePrototypeA.getHobbies());
        System.out.println("原对象的引用类型地址：" + concreatePrototypeA1.getHobbies());
        System.out.println("对象地址比较：" + (concreatePrototypeA.getHobbies() == concreatePrototypeA1.getHobbies()));

    }
}
