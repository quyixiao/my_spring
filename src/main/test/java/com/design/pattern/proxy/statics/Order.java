package com.design.pattern.proxy.statics;

import lombok.Data;

@Data
public class Order {
    private Object orderInfo;
    private Long createTime;
    private String id ;
}
