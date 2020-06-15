package com.test.help3.test37;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test37 {
    public static void main(String[] args) {
        ApplicationContext bf = new ClassPathXmlApplicationContext("classpath:*/spring37.xml");
        System.out.println(((TestA)bf.getBean("testA")).getTestB());
    }
}
