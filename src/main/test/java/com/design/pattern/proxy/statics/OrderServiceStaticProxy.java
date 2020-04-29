package com.design.pattern.proxy.statics;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OrderServiceStaticProxy implements IOrderService {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");


    private IOrderService orderService;

    public OrderServiceStaticProxy(IOrderService orderService) {
        this.orderService = orderService;
    }


    @Override
    public int createOrder(Order order) {
        before();
        long time = order.getCreateTime();
        Integer dbRouter = Integer.valueOf(simpleDateFormat.format(new Date(time)));
        System.out.println("静态代理 类自动分配 【DB_" + dbRouter + "】数据源处理数据");
        DynamicDataSourceEntry.set(dbRouter);
        orderService.createOrder(order);
        after();
        return 0;
    }


    private void before() {
        System.out.println("Proxy before method .");
    }

    private void after() {
        System.out.println("Proxy after method .");
    }
}
