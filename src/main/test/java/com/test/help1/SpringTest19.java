package com.test.help1;

import com.alibaba.fastjson.JSON;
import com.test.LogUtils;
import com.test1.Knife;
import com.test1.KnifeJuggler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Slf4j
// todo 没有解决正确
public class SpringTest19 {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring19.xml");


        KnifeJuggler knifeJuggler = (KnifeJuggler) classPathXmlApplicationContext.getBean("knifeJuggler");
        System.out.println("=============================================" + JSON.toJSONString(knifeJuggler.getKnives()));


        Knife knife1 =  classPathXmlApplicationContext.getBean(Knife.class);
        LogUtils.info("knife1 ="+knife1);
        Knife knife2 =  classPathXmlApplicationContext.getBean(Knife.class);
        LogUtils.info("knife2 ="+knife2);
        Knife knife3 =  classPathXmlApplicationContext.getBean(Knife.class);
        LogUtils.info("knife3 ="+knife3);
    }
}
