package com.test.help3;

import com.test3.cn30.Behavior;
import com.test3.cn30.Liquid1;
import com.test3.cn30.Liquid2;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest30 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring30.xml");
        Behavior behavior = context.getBean(Behavior.class);

        Liquid1 liquid1 = (Liquid1) behavior;
        liquid1.color();


        Liquid2 liquid2 = (Liquid2) behavior;
        liquid2.drink();


        behavior.flow();

    }
}
