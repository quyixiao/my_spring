package com.test.help3;

import com.test3.cn33.BaseConfig33;
import com.test3.cn33.Contestant33;
import com.test3.cn33.UserService33;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringTest33 {

    public static void main(String[] args) {
        ApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(BaseConfig33.class);

       /* Contestant33 contestant33 = applicationContext.getBean(Contestant33.class);
        contestant33.setName();

        System.out.println("=====================================");

        UserService33 userService33 = (UserService33)contestant33;

        userService33.getName();*/

        UserService33 userService33 = applicationContext.getBean(UserService33.class);
        userService33.getName();
        System.out.println("=====================================");

        Contestant33 contestant33 = (Contestant33) userService33;
        contestant33.setName();


     /*
     2020-04-23 17:50:35.244 【INFO】 [AbstractApplicationContext:606] 1044  10.0.0.94 end resetCommonCaches
Exception in thread "main" java.lang.NullPointerException
	at com.test.help3.SpringTest33.main(SpringTest33.java:35)


     Contestant33 contestant33 = ContestantIntroducer.contestant33;
        contestant33.setName();

        System.out.println("=====================================");

        UserService33 userService33 = (UserService33)contestant33;

        userService33.getName();
*/


    }
}
