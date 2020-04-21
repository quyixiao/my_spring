package com.test.help1;

import com.alibaba.fastjson.JSON;
import com.test.LogUtils;
import com.test2.Citys;
import com.test2.OneManBand2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;


@Slf4j
public class SpringTest13 {


    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring13.xml");



        Map kenny = (Map) classPathXmlApplicationContext.getBean("mapcities");
        LogUtils.info(JSON.toJSONString(kenny));

        List cities = (List) classPathXmlApplicationContext.getBean("cities");

        LogUtils.info(JSON.toJSONString(cities));
        Citys citys = (Citys) classPathXmlApplicationContext.getBean("citys");
        LogUtils.info(JSON.toJSONString(citys));
        Citys citys1 = (Citys) classPathXmlApplicationContext.getBean("citys1");
        LogUtils.info(JSON.toJSONString(citys1));

    }
}
