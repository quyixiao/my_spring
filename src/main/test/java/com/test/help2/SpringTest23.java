package com.test.help2;

import com.test2.cn22.BeanConfig;
import com.test2.cn22.UserService;
import com.test2.cn22.impl.ShowServiceImpl;
import com.test2.cn22.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


@Slf4j
// todo 没有解决正确
public class SpringTest23 {

    public static void main(String[] args) throws Exception {


        ApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(BeanConfig.class);
        ShowServiceImpl showService = applicationContext.getBean(ShowServiceImpl.class);
        showService.show("333333333333333333333333333333");


    }
}
