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

package org.springframework.remoting.httpinvoker;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} for HTTP invoker proxies. Exposes the proxied service
 * for use as a bean reference, using the specified service interface.
 *
 * <p>The service URL must be an HTTP URL exposing an HTTP invoker service.
 * Optionally, a codebase URL can be specified for on-demand dynamic code download
 * from a remote location. For details, see HttpInvokerClientInterceptor docs.
 *
 * <p>Serializes remote invocation objects and deserializes remote invocation
 * result objects. Uses Java serialization just like RMI, but provides the
 * same ease of setup as Caucho's HTTP-based Hessian and Burlap protocols.
 *
 * <p><b>HTTP invoker is the recommended protocol for Java-to-Java remoting.</b>
 * It is more powerful and more extensible than Hessian and Burlap, at the
 * expense of being tied to Java. Nevertheless, it is as easy to set up as
 * Hessian and Burlap, which is its main advantage compared to RMI.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setServiceInterface
 * @see #setServiceUrl
 * @see #setCodebaseUrl
 * @see HttpInvokerClientInterceptor
 * @see HttpInvokerServiceExporter
 * @see org.springframework.remoting.rmi.RmiProxyFactoryBean
 * @see org.springframework.remoting.caucho.HessianProxyFactoryBean
 * @see org.springframework.remoting.caucho.BurlapProxyFactoryBean
 */
public class HttpInvokerProxyFactoryBean extends HttpInvokerClientInterceptor
		implements FactoryBean<Object> {

	private Object serviceProxy;


	/***
	 * 分析服务端解析以有处理过程后，我们接下来分析客户端调用的过程，在服务端调用的分析中我们反复提到需要从HttpServletRequest中提取从
	 * 客户端传来的RemoteInvocation实例，然后进行相应的解析，所以，在客户端，一个比较重要的任务就是构建RemoteInvocation实例，
	 * 并传送到服务端，根本配置文件中的信息，我们还是首先锁定HttpInvokerProxyFactoryBean类，并查看其层次结构
	 *
	 *  从层次结构中我们看到，HttpInvokerProxyFactoryBean类同样实现InitializingBean接口同时，又实现了FactoryBean以及MethodInterceptor
	 *  ，这己经是进行处理常谈的问题了，实现这几个接口及这几个新接口在Spring中会有什么作用就不再赘述了，我们还是根据实现的
	 *  InitializingBean接口分析初始化过程中的逻辑
	 *
	 *  	在afterPropertiesSet中主要创建了一个代理 ，该代理二元二次了配置的服务接口，并使用当前类也就是HttpInvokerProxyFactoryBean
	 *  作为增强，都因为HttpInvokerProxyFactoryBean 实现了MethodInterceptor方法，所以可以作为增强拦截器
	 *  同样，又由于HttpInvokerProxyFactoryBean实现了FactoryBean接口，所以通过Spring中普通的方式调用该bean高朋和的并不是该bean的本身
	 *  而是在此类中的getObject方法返回的实例，也就是实例化中的所创建的代理
	 *
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (getServiceInterface() == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(getBeanClassLoader());
	}

	/***
	 * 那么，综合之前的使用示例，我们再次回顾一下，HttpInvokerProxyFactoryBean类型bean在初始化过程中创建了封装，那么综合之前的使用示例
	 * ，我们再次回顾一下，HttpInvokerProxyFactoryBean类型的bean在初始化过程中创建了封装服务接口代理，并使用自身作为增强拦截器，
	 * 然后又因为实例了FactoryBean接口，所以获取Bean的时候返回的其实是创建创建的代理，那么，汇总上面的逻辑，当调用如下代码时，其实就是调用
	 * 代理类中的服务方法，而在调用代理类中的服务方法时双会使用代理类中加入了增强器进行增强
	 * ApplicationContext context  = new ClassPathXmlApplicationContext("classpath:client.xml");
	 * HttpInvokerTest1 httpInvokerTestI = (HttpInvokerTestI)context.getBean("remoteService");
	 * System.out.println(httpInvokeTestI.getTestPo("dddd"));
	 * 这时，所有的逻辑分析其实己经被转向了对于增强也就是HttpInvokerProxyFactoryBean类本身invoke方法的分析
	 * 在分析invoke方法之前，其实我们己经猜出了该方法所提供了主要功能就是将调用信息封装在RemoteInvocation中，发送给服务端并等待返回结果
	 */
	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
