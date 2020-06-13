package com.test1;

import com.test2.cn24.Student;
import com.test2.cn24.StudentAdditionalDetails;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class TestSimpleLoad {

    public static void main(String[] args) {






        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/mytestbean.xml");



        MyTestBean bean = (MyTestBean)context.getBean("myTestBean");
        System.out.println(bean.getTestStr());




    }
}
