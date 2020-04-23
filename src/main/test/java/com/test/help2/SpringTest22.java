package com.test.help2;

import com.test2.cn21.HePerson;
import com.test2.cn21.MyPerson;
import com.test2.cn22.BeanConfig;
import com.test2.cn22.UserService;
import com.test2.cn22.impl.ShowServiceImpl;
import com.test2.cn22.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
// todo 没有解决正确
public class SpringTest22 {

    public static void main(String[] args) throws Exception {

        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:*/spring22.xml");


        UserService userService = classPathXmlApplicationContext.getBean(UserServiceImpl.class);
        userService.selectByUserName();

        ShowServiceImpl showService = classPathXmlApplicationContext.getBean(ShowServiceImpl.class);
        showService.show("333333333333333333333333333333");


    }
}
