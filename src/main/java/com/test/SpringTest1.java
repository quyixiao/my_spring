package com.test;

import com.test.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest1 {

    //https://repo.spring.io/plugins-release/org/apache/tiles/tiles-extras/2.2.2/

    public static void main(String[] args) {
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:spring.xml");
        UserService userService = classPathXmlApplicationContext.getBean(UserService.class);

        userService.query();

    }
}
