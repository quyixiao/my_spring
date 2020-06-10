/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	/****
	 *  至此，已经完成了代理的创建，不管我们之前是否有阅读过 Spring 源码 ，但是或者多少听过 Spring 的源代码，但是都或者少地的听过
	 *  对 Spring 的代理中 JDKProxy 的实现和 CglibProxy实现，Spring 是如何选取的呢？ 网上的介绍到处都是，我们从源代码的角度来分析
	 *   看看 Spring 是如何选择代理的方式的

	 */
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		/**
		 *   从 if 的判断条件中，我们可以看到3个方面影响着 Spring 的判断
		 *   optimize : 用来控制通过 CGLIB 创建代理的代理是否使用了激进的优化策略，除非完全了解AOP 代理是如何处理优化的，否则不推荐用户
		 *  使用这个设置，目前这个属性仅仅用于 CGLIB 代理，对于 JDK 动态代理缺省的代理无效
		 *  proxyTargetClass 这个属性为 true 时，目标类本身被代理而不是目标类接口，如果这个属性值设置为 true ，CGLIB  代理将被创建，
		 *  设置方式 : <aop:aspectj-autoproxy proxy-target-class="true"></aop>
		 *  hasNoUserSuppliedProxyInterfaces:是否存在代理接口
		 *  下面对 JDK 与 CGLIB  代理方式的总结
		 *  如果目标对象实现了接口，默认情况下会采用 JDK 的动态代理来实现AOP
		 *  如果目标对象实现了接口，可以强制使用 CGLIB 实现 AOP
		 *  如果目标对象没有实现接口，必须采用 CGLIB 库，Spring 会自动的在 JDK 动态代理和 CGLIB 代理之间进行转换
		 *  如何强制使用 CGLIB 实现 AOP 呢？
		 *  (1). 添加 CGBLIB 库，Spring_HOME/cblib/*.jar
		 *  (2).在 Spring 配置文件中加入了<aop:aspectj-auto-proxy proxy-target-class="true" />
		 *  JDK 动态代理和 CGLIB 字节码生成区别？
		 *  JDK 动态代理只能对实现了接口的类生成代理，而不能针对类
		 *  CGLIB 是针对类实现代理，主要是对指定的类生成一个子类，覆盖其中的方法，因为是继承，所以该类或方法最好不要声明成 final
		 *  2. 获取代理
		 *  有写了使用哪种代理方式后便可以进行代理的创建了，但是创建前有必要回顾一下两种方式的使用方法
		 *  JDK 代理的使用示例
		 *  创建业务接口，业务对外提供了接口，包含着业务可以对外提供的功能
		 *  public interface UserService {
		 *  	public abstract void add();
		 *  }
		 *
		 * 创建业务接口的实现类
		 * public class UserServiceImpl implements UserService {
		 * 		public void add (){
		 * 			System.out.println("---------------------------add-----------------------");
		 * 		}
		 * }
		 *	创建自定义的 InvocationHandler ，用于对接口提供的方法进行增强
		 *
		 * public class MyInvocationHandler implements InvocationHandler{
		 * 		private Object target ; //目标对象
		 * 		// 构造方法，@param target 目标对象
		 * 		public MyInvocationHandler(Object target ){
		 * 			super();
		 * 			this.target = target ;
		 * 		}
		 * }
		 * // 执行目标对象的方法
		 * public Object invoke(Object proxy ,Method method ,Object [] args ) throw Throwable {
		 * 		// 在目标对象的方法执行之前简单的打印一下
		 * 		System.out.println("---------------before--------------");
		 * 		// 执行目标对象的方法
		 * 		Object result = method.invoke(target,args);
		 * 		//在目标对象方法执行之后简单的打印一下
		 * 		System.out.println("----------------after-----------------------");
		 * 		return result ;
		 * }
		 * 获取目标对象的代理对象
		 * 	public Object getProxy(){
		 * 		return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),target.getClass().getInterfaces(),this);
		 * 	}
		 *
		 * 	 最后进行测试，验证对于接口的增强是否起到作用
		 * 	 public class ProxyTest{
		 * @test
		 * 		public void testProxy() throws Throwable{
		 * 			// 实例化目标对象
		 * 			UserService userservice = new UserServiceImpl();
		 * 			// 实例化 InvocationHandler
		 * 			MyInvocationHandler invocationHandler = new MyInvocationHandler(userService);
		 * 			//根据目标对象生成代理对象
		 * 			UserService proxy = (UserService) invocationHandler.getProxy();
		 * 			// 调用代理对象方法
		 * 			proxy.add()
		 * 		}
		 * 	 }
		 * 	 执行结果如下：
		 * 	 	-------------------before----------------------
		 * 	 --------------------------add-----------------------
		 * 	 -------------------after--------------------
		 * 	 用起来很简单，其实这个基本上就是 AOP 的一个简单的实现了，在目标对象的方法执行之前和执行之后进行了增强，Spring 的AOP 实现了
		 * 	 其实也是用了 Proxy 和 InvocationHandler 这两个东西的
		 * 	 我们再次来回顾一下使用 JDK 代理的方式，在整个创建的过程中，对于 InvocationHandler 的创建是最核心的，在自定义的 InvocationHandler 中
		 * 	 需要重写3个函数
		 * 	 构造函数，将代理的对象传入
		 * 	 invoke 方法，此方法中实现了 AOP 增强的所有的逻辑
		 * 	 getProxy 方法，此方法千篇一律，但是必不可少
		 * 	 那么，我们来看看 Spring 中的 JDK 代理实现是不是也是这样做的呢？ 继续之前的跟踪，到达了 JDKDynamicAopProxy 的 getProxy
		 *
		 *  */
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 父类.class.isAssignableFrom(子类.class)
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
