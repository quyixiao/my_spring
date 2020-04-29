package com.design.pattern.proxy.statics;

public class OrderServiceImpl implements IOrderService {

    private OrderDao orderDao;

    public OrderServiceImpl() {
        this.orderDao =    new OrderDao();
    }

    @Override
    public int createOrder(Order order) {
        System.out.println("order Service 调用orderDao 创建订单");
        return orderDao.insert(order);
    }
}
