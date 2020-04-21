package com.test.help1;

import com.test.Performer;
import com.test2.OneManBand2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest12 {


    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring12.xml");



        OneManBand2 kenny = (OneManBand2) classPathXmlApplicationContext.getBean("hank");


    }
}
