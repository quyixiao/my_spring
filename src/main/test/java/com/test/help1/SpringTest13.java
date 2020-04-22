package com.test.help1;

import com.alibaba.fastjson.JSON;
import com.test.LogUtils;
import com.test2.*;
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

        Settings settings = (Settings) classPathXmlApplicationContext.getBean("settings");
        System.out.println(settings.getName());


        Settings settings2 = (Settings) classPathXmlApplicationContext.getBean("settings2");
        System.out.println(settings2.getName());



        Settings settings3 = (Settings) classPathXmlApplicationContext.getBean("settings3");
        System.out.println(settings3.getName());

        CityList cityList = (CityList) classPathXmlApplicationContext.getBean("cityList");
        System.out.println(JSON.toJSONString(cityList));


        CityList cityList1 = (CityList) classPathXmlApplicationContext.getBean("cityList1");
        System.out.println(JSON.toJSONString(cityList1));


        CityList cityList2 = (CityList) classPathXmlApplicationContext.getBean("cityList2");
        System.out.println(JSON.toJSONString(cityList2));


        CityNameList cityNameList = (CityNameList) classPathXmlApplicationContext.getBean("cityNameList");
        System.out.println(JSON.toJSONString(cityNameList));


        CityNameList cityNameList1 = (CityNameList) classPathXmlApplicationContext.getBean("cityNameList1");
        System.out.println(JSON.toJSONString(cityNameList1));
    }
}
