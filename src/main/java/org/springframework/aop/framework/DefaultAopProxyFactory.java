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
		 *
		 *  */
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
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
