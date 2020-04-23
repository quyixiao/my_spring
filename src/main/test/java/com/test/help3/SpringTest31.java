package com.test.help3;

import com.test3.cn31.BaseBean31;
import com.test3.cn31.UserService31;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringTest31 {

    public static void main(String[] args) {
        ApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(BaseBean31.class);

        UserService31 userService31 = applicationContext.getBean(UserService31.class);
        userService31.getName();

    }
}
