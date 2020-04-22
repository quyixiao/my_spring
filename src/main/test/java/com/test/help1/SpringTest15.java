package com.test.help1;

import com.test3.InstrumentTest;
import com.test3.InstrumentTest1;
import com.test3.InstrumentTest2;
import com.test3.InstrumentTest3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest15 {


    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring15.xml");










        InstrumentTest3 instrumentTest3 = (InstrumentTest3) classPathXmlApplicationContext.getBean("instrumentTest3");
        System.out.println(instrumentTest3.getInstrument());


    }
}
