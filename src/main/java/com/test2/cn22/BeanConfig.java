package com.test2.cn22;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan("com.test2.cn22")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class BeanConfig {



}