package com.test.help2;

import com.test2.cn20.AnimalFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
// todo 没有解决正确
public class SpringTest20 {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help2/spring20.xml");

        AnimalFactory animalFactory = classPathXmlApplicationContext.getBean(AnimalFactory.class);
        animalFactory.getAnimal().eat();

    }
}
