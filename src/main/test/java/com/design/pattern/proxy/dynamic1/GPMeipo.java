package com.design.pattern.proxy.dynamic1;

import java.lang.reflect.Method;

public class GPMeipo implements GPInvocationHandler {
    private Object target;


    public Object getInstance(Object target) throws Exception {
        this.target = target;
        Class<?> clazz = target.getClass();
        return GPProxy.newProxyInstance(new GPClassLoader(), clazz.getInterfaces(), this);

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("我是媒婆，我要给你找对象，现在已经确认你的要求了");
        Object obj = method.invoke(this.target, args);
        System.out.println("如果合适的话，就准备办事");
        return obj;
    }


}
