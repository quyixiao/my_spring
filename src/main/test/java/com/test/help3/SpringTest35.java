package com.test.help3;

import com.test3.cn35.AnnotationAspect;
import com.test3.cn35.AnnotationService;
import org.apache.tools.ant.types.resources.FileResourceIterator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class SpringTest35 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring35.xml");


        System.out.println("=========================================================================");
        AnnotationAspect aspect = context.getBean(AnnotationAspect.class);
        System.out.println(aspect);
        AnnotationService annotationService = context.getBean(AnnotationService.class);
        annotationService.save();
        System.out.println("=========================================================================");

    }
}
