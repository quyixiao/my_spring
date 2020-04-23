package com.test.help2;

import com.test2.cn26.Tree;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest26 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring26.xml");

        Tree phone = context.getBean(Tree.class);
        phone.raise();
    }
}
