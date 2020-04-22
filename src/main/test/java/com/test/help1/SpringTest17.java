package com.test.help1;

import com.test3.InstrumentTest4;
import com.test4.InstrumentTest5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest17 {


    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring17.xml");










        InstrumentTest5 instrumentTest5 = (InstrumentTest5) classPathXmlApplicationContext.getBean("instrumentTest5");
        System.out.println(instrumentTest5.getInstrument());


    }
}
