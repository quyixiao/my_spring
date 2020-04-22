package com.test.help1;

import com.test3.InstrumentTest3;
import com.test3.InstrumentTest4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest16 {


    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring16.xml");










        InstrumentTest4 instrumentTest4 = (InstrumentTest4) classPathXmlApplicationContext.getBean("instrumentTest4");
        System.out.println(instrumentTest4.getInstrument());


    }
}
