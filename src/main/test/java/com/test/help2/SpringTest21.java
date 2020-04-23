package com.test.help2;

import com.hazelcast.config.ClasspathXmlConfig;
import com.test2.cn21.HePerson;
import com.test2.cn21.MyPerson;
import com.test2.cn21.Person;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
// todo 没有解决正确
public class SpringTest21 {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:*/spring21.xml");

        MyPerson myPerson = classPathXmlApplicationContext.getBean(MyPerson.class);
        System.out.println(" myPerson :" + myPerson);
        System.out.println(" myPerson.getPerson() :" + myPerson.getPerson());
        myPerson.getPerson().hair();


        HePerson person = classPathXmlApplicationContext.getBean(HePerson.class);
        System.out.println("==============="+person);

    }
}
