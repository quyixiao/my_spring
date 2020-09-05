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

package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;

/**
 * Convenient superclass for RMI-based remote exporters. Provides a facility
 * to automatically wrap a given plain Java service object with an
 * RmiInvocationWrapper, exposing the {@link RmiInvocationHandler} remote interface.
 *
 * <p>Using the RMI invoker mechanism, RMI communication operates at the {@link RmiInvocationHandler}
 * level, sharing a common invoker stub for any number of services. Service interfaces are <i>not</i>
 * required to extend {@code java.rmi.Remote} or declare {@code java.rmi.RemoteException}
 * on all service methods. However, in and out parameters still have to be serializable.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see RmiServiceExporter
 * @see JndiRmiServiceExporter
 */
public abstract class RmiBasedExporter extends RemoteInvocationBasedExporter {

	/**
	 * Determine the object to export: either the service object itself
	 * or a RmiInvocationWrapper in case of a non-RMI service object.
	 * @return the RMI object to export
	 * @see #setService
	 * @see #setServiceInterface
	 * 2.初始化要导出的实例对象
	 * 之前提到过，当请求的某个RMI服务的时候，RMI会根据注册的服务名称，将请求引导至远程对象处理类中，这个处理类便是使用getObjectToExport()进行创建
	 *
	 */
	protected Remote getObjectToExport() {
		// determine remote object
		// 如果配置的service属性对应的的类实现了Remote接口且没有配置serviceInterface属性, 那么直接使用 service 作为处理类，否则使用
		// RMIInvocationWrapper 对 service 的代理类和当前类也就是 RMIServiceExporter 进行封装
		// 经过这样的封装，客户端与服务端便可以达成一致的协义，当客户端检测到的 RMIInvocationWrapper 类型 stub 的时候便会直接调用其
		// invoke方法，使得调用与服务端很好的连接在一起了，而 RMIInvocationWrapper 封装了用于处理请求的代理类，在 invoke 中便会使用
		// 代理类进行进一步的处理
		if (getService() instanceof Remote &&
				(getServiceInterface() == null || Remote.class.isAssignableFrom(getServiceInterface()))) {
			// conventional RMI service
			return (Remote) getService();
		}
		else {
			// RMI invoker
			if (logger.isDebugEnabled()) {
				logger.debug("RMI service [" + getService() + "] is an RMI invoker");
			}
			// 对service进行封装
			return new RmiInvocationWrapper(getProxyForService(), this);
		}
	}

	/**
	 * Redefined here to be visible to RmiInvocationWrapper.
	 * Simply delegates to the corresponding superclass method.
	 */
	@Override
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		return super.invoke(invocation, targetObject);
	}

}
