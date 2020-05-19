/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.config;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} for the {@code aop} namespace.
 *
 * <p>Provides a {@link BeanDefinitionParser} for the
 * {@code &lt;aop:config&gt;} tag. A {@code config} tag can include nested
 * {@code pointcut}, {@code advisor} and {@code aspect} tags.
 *
 * <p>The {@code pointcut} tag allows for creation of named
 * {@link AspectJExpressionPointcut} beans using a simple syntax:
 * <pre class="code">
 * &lt;aop:pointcut id=&quot;getNameCalls&quot; expression=&quot;execution(* *..ITestBean.getName(..))&quot;/&gt;
 * </pre>
 *
 * <p>Using the {@code advisor} tag you can configure an {@link org.springframework.aop.Advisor}
 * and have it applied to all relevant beans in you {@link org.springframework.beans.factory.BeanFactory}
 * automatically. The {@code advisor} tag supports both in-line and referenced
 * {@link org.springframework.aop.Pointcut Pointcuts}:
 *
 * <pre class="code">
 * &lt;aop:advisor id=&quot;getAgeAdvisor&quot;
 *     pointcut=&quot;execution(* *..ITestBean.getAge(..))&quot;
 *     advice-ref=&quot;getAgeCounter&quot;/&gt;
 *
 * &lt;aop:advisor id=&quot;getNameAdvisor&quot;
 *     pointcut-ref=&quot;getNameCalls&quot;
 *     advice-ref=&quot;getNameCounter&quot;/&gt;</pre>
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * Register the {@link BeanDefinitionParser BeanDefinitionParsers} for the
	 * '{@code config}', '{@code spring-configured}', '{@code aspectj-autoproxy}'
	 * and '{@code scoped-proxy}' tags.
	 * 我们知道，使用面向对象编程有一些弊端，当需要为多个不具有继承关系的对象引入同一个公共行为时，例如日志，安全检测等，我们只有在每个对象
	 * 里引用公共的行为， 这样程序就产生了大量的重复的代码，程序就不便于维护了，所以就有了一个对面向对象编程的补充，即面向方面编程，AOP
	 * 所关注的是方向是横向的，不同于 OOP是纵向的
	 * Spring 中提供了 AOP 的实现，但是在低版本 Spring 中定义一个切面是比较麻烦的，需要实现特定的接口，并进行一些较为复杂的配置，低版本的
	 * Spring  AOP 的配置是被批评最多的，Spring 听取了这方面的批评的声音，并下决心彻底的改变这一现状，Spring AOP 已经焕然一新了，你可以
	 * 使用@AspectJ 注解非常容易的定义一个切面，不需要实现任何的接口
	 * Spring 2.0 采用了@AspectJ 注册 POJO 进行标注，从而定义一个包含切点信息和增强的横切逻辑的切面表达式语法进行切点定义，可以通过
	 * 切点函数，运算符，通配符，等高级功能进行切点定义，拥有强大的连接点描述能力，我们先来直观的浏览一下 Spring AOP 的实现
	 *
	 * 7.动态 AOP  使用的示例
	 * 在实际工作中，此 Bean 可能是满足业务的需要的核心逻辑，例如 test 方法中的可能会封装着某个核心业务，但是我们想在 test 前后加入日志来
	 * 跟踪调试，如果直接修改源码并不符合面向对象的设计方法，而且随意的改动原有的代码也会造成一定的风险，还好接下来Spring 帮我们做到了这一点
	 * public class TestBean{
	 *     private String testStr = "testStr";
	 *
	 *     public String getTestStr(){
	 *         return testStr;
	 *     }
	 *
	 *     public void setTestStr(String testStr){
	 *         this.testStr = testStr ;
	 *     }
	 *
	 *     public void test(){
	 *         System.out.println("test");
	 *     }
	 * }
	 *
	 * Spring 中摒弃了最原始的繁杂的配置方式而采用@AspectJ 注解对 POJO 进行标注，使用 AOP 的工作大大简化，例如，在 AspectJTest
	 * 类中，我们需要做的就是在所有的类的 test 方法执行前在控制台中打印 beforeTest ，而在所有的类 test 方法执行后打印 afterTest ，
	 *  同时又使用环绕方式在所有的类的方法执行前后再次分别打印 before1 和 after1
	 * @Aspect
	 * public class AspectJTest {
	 *
	 * @Pointcut("execution(* *.test(..))")
	 * public void test(){
	 *
	 * }
	 *
	 *
	 * @before("test()")
	 * public void beforeTest(){
	 *     System.out.println("beforeTest");
	 * }
	 *
	 * @After("test()")
	 * public void afterTest(){
	 *     System.out.println("afterTest");
	 * }
	 *
	 * @Around("test()")
	 * public Object arountTest(ProceedingJoinPoint p){
	 *     System.out.println("before1");
	 *     Object o = null;
	 *     try{
	 *         o = p.proceed();
	 *     }catch(Throwable e ){
	 *         e.printStackTrack();
	 *     }
	 *     System.out.println("after1");
	 *     return o;
	 * }
	 *
	 * }
	 *
	 *
	 * 3.创建配置文件
	 * XML  是 Spring 的基础，尽管 Spring 一再简化配置，并且在有使用注解取代 XML 配置之势，但是无论如何，至少现在的 XML 还是 Spring 为基础的
	 * 要在 Spring 中开启 AOP 功能，还需要
	 * 在配置文件中作如下声明
	 *
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xmlns:context="http://www.springframework.org/schema/context"
	 *        xmlns:aop="http://www.springframework.org/schema/aop"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
	 *         http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd
	 *         http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-3.0.xsd">
	 *
	 *     <context:annotation-config/>
	 *     <aop:aspectj-autoproxy proxy-target-class="true"/>
	 *     <context:component-scan base-package="com.*"></context:component-scan>
	 *     <bean id="test" class="test.TestBean"></bean>
	 *     <bean class="test.AspectJtest"></bean>
	 *
	 *  </beans>
	 *
	 *  经过以上步骤后，便可以验证 Spring 的 AOP 为我们提供了神奇的效果了
	 *  public static void main(String [] args ){
	 *  	ApplicationContext br = new ClassPathXmlApplicationContext("aspectTest.xml");
	 *  	TestBean bean = (TestBean) bf.getBean("test");
	 *  	bean.test();
	 *  }
	 *	不出意外 ，我们会看到控制台中打印了如下的代码了
	 *	beforeTest
	 *	before1
	 *	test
	 *	afterTest
	 *	after1
	 *	Spring  实现了对所有类的 test 方法进行增强，使辅助功能可以独立于核心业务之外，方便与程序的扩展和解耦
	 * 那么 Spring ，究竟是如何实现 AOP 的呢？  首先我们知道，Spring 是否支持注解的 AOP 是由一个配置文件控制的，也就是说
	 * <aop:aspectj-autoproxy/> ，当配置文件中的声明了这句配置的时候，Spring 就会支持注解的 AOP ,那么我们分析就从这句注解开始
	 *
	 *  动态 AOP 自定义标签
	 *  之前讲过 Spring 中的自定义的注解，如果声明了自定义注解，那么就一定会在程序中的某个地方注册了对应的解析器，我们搜索了整个代码
	 *  ，尝试找到注册的地方，全局搜索后我们发现了 AOPNamespaceHandler 中对应了这样的一段函数：如下：
	 *	此处，不再对 Spring 中定义的注解方式进行讨论，有兴趣的读者可以回顾之前的内容，我们可以得知，在解析配置文件的时候，一旦遇到
	 *aspectj-autoproxy 注解时就会使用解析器 AspectJAutoProxyBeanDefinitionParser 进行解析，那么我们看看 AspectJAutoProxyBeanDefinitionParser 的内部实现
	 */
	@Override
	public void init() {
		// In 2.0 XSD as well as in 2.1 XSD.
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace as of 2.1
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}
