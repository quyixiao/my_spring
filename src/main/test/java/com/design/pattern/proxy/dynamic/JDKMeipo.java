package com.design.pattern.proxy.dynamic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JDKMeipo implements InvocationHandler {

    private Object target;

    public Object getInstance(Object target) throws Exception {
        this.target = target;
        Class<?> clazz = target.getClass();
        return Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before();
        System.out.println(method.getName());
        Object obj = method.invoke(this.target, args);

        after();
        return null;
    }


    private void before() {
        System.out.println("我是媒婆，我要给你物色对象，现在已经确认你的需求了，开始物色");
    }

    private void after() {
        System.out.println("如果合适的话，就准备办事");
    }

}
