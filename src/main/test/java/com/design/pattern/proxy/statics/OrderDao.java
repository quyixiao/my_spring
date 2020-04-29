package com.design.pattern.proxy.statics;

public class OrderDao {
    public int insert(Order order) {
        System.out.println("OrderDao 创建Order 成功");
        return 1;
    }
}
