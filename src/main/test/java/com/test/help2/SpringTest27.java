package com.test.help2;

import com.test2.cn27.Peple;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest27 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring27.xml");
        Peple phone = context.getBean(Peple.class);
        phone.getName();
    }
}
