package com.test3.cn31;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan("com.test3.cn31")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class BaseBean31 {
}
