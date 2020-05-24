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

package org.springframework.remoting.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for classes that export a remote service.
 * Provides "service" and "serviceInterface" bean properties.
 *
 * <p>Note that the service interface being used will show some signs of
 * remotability, like the granularity of method calls that it offers.
 * Furthermore, it has to have serializable arguments etc.
 *
 * @author Juergen Hoeller
 * @since 26.12.2003
 */
public abstract class RemoteExporter extends RemotingSupport {

	private Object service;

	private Class<?> serviceInterface;

	private Boolean registerTraceInterceptor;

	private Object[] interceptors;


	/**
	 * Set the service to export.
	 * Typically populated via a bean reference.
	 */
	public void setService(Object service) {
		this.service = service;
	}

	/**
	 * Return the service to export.
	 */
	public Object getService() {
		return this.service;
	}

	/**
	 * Set the interface of the service to export.
	 * The interface must be suitable for the particular service and remoting strategy.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Return the interface of the service to export.
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * Set whether to register a RemoteInvocationTraceInterceptor for exported
	 * services. Only applied when a subclass uses {@code getProxyForService}
	 * for creating the proxy to expose.
	 * <p>Default is "true". RemoteInvocationTraceInterceptor's most important value
	 * is that it logs exception stacktraces on the server, before propagating an
	 * exception to the client. Note that RemoteInvocationTraceInterceptor will <i>not</i>
	 * be registered by default if the "interceptors" property has been specified.
	 * @see #setInterceptors
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 */
	public void setRegisterTraceInterceptor(boolean registerTraceInterceptor) {
		this.registerTraceInterceptor = Boolean.valueOf(registerTraceInterceptor);
	}

	/**
	 * Set additional interceptors (or advisors) to be applied before the
	 * remote endpoint, e.g. a PerformanceMonitorInterceptor.
	 * <p>You may specify any AOP Alliance MethodInterceptors or other
	 * Spring AOP Advices, as well as Spring AOP Advisors.
	 * @see #getProxyForService
	 * @see org.springframework.aop.interceptor.PerformanceMonitorInterceptor
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors = interceptors;
	}


	/**
	 * Check whether the service reference has been set.
	 * @see #setService
	 */
	protected void checkService() throws IllegalArgumentException {
		if (getService() == null) {
			throw new IllegalArgumentException("Property 'service' is required");
		}
	}

	/**
	 * Check whether a service reference has been set,
	 * and whether it matches the specified service.
	 * @see #setServiceInterface
	 * @see #setService
	 */
	protected void checkServiceInterface() throws IllegalArgumentException {
		Class<?> serviceInterface = getServiceInterface();
		Object service = getService();
		if (serviceInterface == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		if (service instanceof String) {
			throw new IllegalArgumentException("Service [" + service + "] is a String " +
					"rather than an actual service reference: Have you accidentally specified " +
					"the service bean name as value instead of as reference?");
		}
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException("Service interface [" + serviceInterface.getName() +
					"] needs to be implemented by service [" + service + "] of class [" +
					service.getClass().getName() + "]");
		}
	}

	/**
	 * Get a proxy for the given service object, implementing the specified
	 * service interface.
	 * <p>Used to export a proxy that does not expose any internals but just
	 * a specific interface intended for remote access. Furthermore, a
	 * {@link RemoteInvocationTraceInterceptor} will be registered (by default).
	 * @return the proxy
	 * @see #setServiceInterface
	 * @see #setRegisterTraceInterceptor
	 * @see RemoteInvocationTraceInterceptor
	 * 请求处理类的初始化主要处理规则为：如果配置了service属性对应的类实现了Remote接口且没有配置serviceInterface属性，那么直接使用
	 * service作为处理类，否则使用RMIInvocationWrapper对service的代理类和当前类也就是RMIServiceExporter进行封装
	 * 经过这样的封装，客户端与服务端可以达成一致的协义，当客户端很好的连接在一起了，而RMIInvocationRrapper封装了用于处理请求的代理类
	 * 在invoke中便会使用代理类进行进一步处理
	 * 之前的逻辑已经非常的清楚了，当请求RMI服务时会由注册表Registry实例将请求转向之前的注册的处理类去处理，也就是之前封装的RMIInvocationWrapper
	 * ,然后由RMIInvocationWrapper中的invoke方法进行处理，那么为什么不是在invoke方法中直接使用service ，而是通过代理再次将service封装呢？
	 * 这其中一个关键点就是在创建代理时添加了一个增强拦截器RemoteInvocationTraceInterceptor目的是为了对方法的调用进行打印跟踪，但是如果直接在invoke方法中硬编码这些
	 * 日志，会使得代码看起来，而且耦合度很高，使用代理的方式就会解决这样的问题，而且会有很高的可扩展性
	 *
	 *
	 * |
	 * 通过上面的3个方法串联，可以看到，初始化的过程实现的逻辑的主要是创建一个代理，代理中封装了对于特定的请求处理方法以及接口等信息，
	 * 而这个代理的最关键的目的就是加入了RemoteInvocationTraceInterceptor增强器，当然 创建代理还有个好处，比如代码优雅，方便扩展，
	 * RemoteInvocationTraceInterceptor中增强的主要是对增强的目标方法进行一些相关的信息日志打印，并没有在此基础上进行任何的功能性的
	 * 增强，那么这个代理究竟是在什么时候使用呢？暂时留下悬念，我们接下来分析当前有Web请求时，HttpRequestHandler的HandleRequest的处理方法
	 * 2.处理来自客户端的request
	 * 当有Web请求时，根据配置中的规则会把路径匹配的访问直接引入到对应的HttpRequestHandler中，本例中Web请求与普通的Web请求是有区别的，
	 * 因此此处的请求包含着HttpInvoke的处理过程
	 *
	 *
	 *
	 */
	protected Object getProxyForService() {
		// 验证service
		checkService();
		// 验证serviceInterface
		checkServiceInterface();
		// 使用JDK创建代理
		ProxyFactory proxyFactory = new ProxyFactory();
		//添加代理接口
		proxyFactory.addInterface(getServiceInterface());
		if (this.registerTraceInterceptor != null ?
				this.registerTraceInterceptor.booleanValue() : this.interceptors == null) {
			// 加入代理的横切面RemoteInvocationTraceInterceptor并记录Exporter名称
			proxyFactory.addAdvice(new RemoteInvocationTraceInterceptor(getExporterName()));
		}
		if (this.interceptors != null) {
			AdvisorAdapterRegistry adapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
			for (int i = 0; i < this.interceptors.length; i++) {
				proxyFactory.addAdvisor(adapterRegistry.wrap(this.interceptors[i]));
			}
		}
		// 设置要代理的目标类
		proxyFactory.setTarget(getService());
		proxyFactory.setOpaque(true);
		// 创建代理
		return proxyFactory.getProxy(getBeanClassLoader());
	}

	/**
	 * Return a short name for this exporter.
	 * Used for tracing of remote invocations.
	 * <p>Default is the unqualified class name (without package).
	 * Can be overridden in subclasses.
	 * @see #getProxyForService
	 * @see RemoteInvocationTraceInterceptor
	 * @see ClassUtils#getShortName
	 */
	protected String getExporterName() {
		return ClassUtils.getShortName(getClass());
	}

}
