package com.design.pattern;

import com.alibaba.fastjson.JSON;

import java.net.URL;
import java.util.Enumeration;

public class Test2 {

    public static void main(String[] args)  throws Exception{
        String path = "com/test3/cn35";
        Enumeration<URL> resourceUrls = ClassLoader.getSystemResources(path);
        System.out.println(JSON.toJSONString(resourceUrls));
    }
}
