package com.test;

import com.test.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest1 {


    public static void main(String[] args) {
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:spring.xml");
        UserService userService = classPathXmlApplicationContext.getBean(UserService.class);

        userService.query();

    }
}
