package com.test.help2;

import com.test2.cn27.Peple;
import com.test2.cn28.Thinker;
import com.test2.cn28.impl.Volunteer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest28 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring28.xml");




        Thinker thinker = context.getBean(Thinker.class);
        thinker.thinkOfSomething("quyixiao quyixiao xx quyixiao");
    }
}
