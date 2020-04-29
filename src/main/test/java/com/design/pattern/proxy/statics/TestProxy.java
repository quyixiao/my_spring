package com.design.pattern.proxy.statics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestProxy {

    public static void main(String[] args) {
        try {
            Order order = new Order();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
            Date date = simpleDateFormat.parse("2017/02/10");
            order.setCreateTime(date.getTime());

            IOrderService orderService = new OrderServiceStaticProxy(new OrderServiceImpl());
            orderService.createOrder(order);
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }
}
