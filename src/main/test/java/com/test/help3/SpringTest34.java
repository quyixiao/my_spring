package com.test.help3;

import com.test3.cn34.Person34;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest34 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring34.xml");


        Person34 person34 = context.getBean(Person34.class);
        person34.perform();
    }
}
