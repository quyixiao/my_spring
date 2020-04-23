package com.test.help2;

import com.test2.cn25.Phone;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest25 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring25.xml");

        Phone phone = context.getBean(Phone.class);
        phone.call();
    }
}
