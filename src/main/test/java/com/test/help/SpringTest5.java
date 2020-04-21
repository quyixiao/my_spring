package com.test.help;

import com.test.LogUtils;
import com.test.Stage;
import com.test.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest5 {

    //https://repo.spring.io/plugins-release/org/apache/tiles/tiles-extras/2.2.2/

    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:spring5.xml");

        LogUtils.info(" bean init finish .......................");

        Ticket theStage1 = (Ticket)classPathXmlApplicationContext.getBean("ticket");
        System.out.println(theStage1);
        Ticket theStage2 = (Ticket)classPathXmlApplicationContext.getBean("ticket");
        System.out.println(theStage2);

    }
}
