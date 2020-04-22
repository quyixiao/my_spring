package com.test.help1;

import com.test4.InstrumentTest5;
import com.test4.InstrumentTest6;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
// todo 没有解决正确
public class SpringTest18 {

    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring18.xml");


        InstrumentTest6 instrumentTest6 = (InstrumentTest6) classPathXmlApplicationContext.getBean("instrumentTest6");
        System.out.println(instrumentTest6.getInstrument());


    }
}
