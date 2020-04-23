package com.test.help3;

import com.test3.cn32.BaseBean32;
import com.test3.cn32.Person32;
import com.test3.cn32.Thinker32;
import com.test3.cn32.Woman32;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringTest32 {

    public static void main(String[] args) {
        ApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(BaseBean32.class);


        Thinker32 thinker32 = applicationContext.getBean(Thinker32.class);
        thinker32.thinkOfSomething(" 我想吃饭 ");

        Person32 person32 = applicationContext.getBean(Person32.class);
        person32.name(" 瞿贻晓 ");
        person32.setAge(32);
        person32.setWife(new Woman32("小胡",29));
    }
}
