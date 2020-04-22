package com.test.help1;

import com.alibaba.fastjson.JSON;
import com.test.LogUtils;
import com.test2.CityList;
import com.test2.CityNameList;
import com.test2.Citys;
import com.test2.Settings;
import com.test3.InstrumentTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;


@Slf4j
public class SpringTest14 {


    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext classPathXmlApplicationContext
                = new ClassPathXmlApplicationContext("classpath:help1/spring14.xml");



        InstrumentTest instrumentTest = (InstrumentTest) classPathXmlApplicationContext.getBean("instrumentTest");
        System.out.println(instrumentTest.getInstrument());




    }
}
