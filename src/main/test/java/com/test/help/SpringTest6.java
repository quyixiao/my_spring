package com.test.help;

import com.test.LogUtils;
import com.test.Ticket;
import com.test2.Auditorium;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest6 {

    //https://repo.spring.io/plugins-release/org/apache/tiles/tiles-extras/2.2.2/

    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:spring6.xml");


        Auditorium auditorium = (Auditorium) classPathXmlApplicationContext.getBean("auditorium");





    }
}
