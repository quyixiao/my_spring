package com.test.help;

import com.test.Performer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest8 {

    //https://repo.spring.io/plugins-release/org/apache/tiles/tiles-extras/2.2.2/

    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:spring8.xml");


        Performer kenny = (Performer) classPathXmlApplicationContext.getBean("kenny");
        kenny.perform();




    }
}
