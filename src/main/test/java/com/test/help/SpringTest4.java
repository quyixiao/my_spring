package com.test.help;

import com.test.LogUtils;
import com.test.Performer;
import com.test.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
public class SpringTest4 {

    //https://repo.spring.io/plugins-release/org/apache/tiles/tiles-extras/2.2.2/

    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:spring4.xml");

        LogUtils.info(" bean init finish .......................");

        Stage theStage = (Stage)classPathXmlApplicationContext.getBean("theStage");
    }
}
