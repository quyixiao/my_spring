package com.test.help2;

import com.test2.cn24.Student;
import com.test2.cn24.StudentAdditionalDetails;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest24 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring24.xml");

        StudentAdditionalDetails studentAdditionalDetails = (StudentAdditionalDetails) context.getBean("studentAdditionalDetails");
        ((Student) studentAdditionalDetails).showDetails();


        studentAdditionalDetails.showAdditionalDetails();
    }
}
