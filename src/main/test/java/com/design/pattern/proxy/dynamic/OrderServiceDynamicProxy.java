package com.design.pattern.proxy.dynamic;


import com.design.pattern.proxy.statics.IOrderService;
import com.design.pattern.proxy.statics.Order;
import com.design.pattern.proxy.statics.OrderServiceImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OrderServiceDynamicProxy implements InvocationHandler {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");

    private Object target;


    public Object getInstance(Object target) {
        this.target = target;
        Class<?> clazz = target.getClass();
        return java.lang.reflect.Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before(args[0]);
        //System.out.println(proxy.getClass().getName());
        System.out.println(args[0].getClass().getName());
        Object object = method.invoke(target, args);

        after();
        return object;
    }

    private void after() {
        System.out.println("Proxy after method.");
    }

    private void before(Object target) {
        try {
            System.out.println(" Proxy before method");
            long time = (Long) target.getClass().getMethod("getCreateTime").invoke(target);
            Integer dbRouter = Integer.valueOf(simpleDateFormat.format(new Date(time)));
            System.out.println("静态代理类自动分配到【DB_" + dbRouter + "】数据源处理数据");
            // DynamicDataSourceEntry.set(dbRouter);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        try {
            Order order = new Order();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Date date = sdf.parse("2018/02/01");
            order.setCreateTime(date.getTime());
            IOrderService orderService = (IOrderService) new OrderServiceDynamicProxy().getInstance(new OrderServiceImpl());
            orderService.createOrder(order);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
