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
import java.rmi.RemoteException;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.util.Assert;

/**
 * Server-side implementation of {@link RmiInvocationHandler}. An instance
 * of this class exists for each remote object. Automatically created
 * by {@link RmiServiceExporter} for non-RMI service implementations.
 *
 * <p>This is an SPI class, not to be used directly by applications.
 *
 * @author Juergen Hoeller
 * @since 14.05.2003
 * @see RmiServiceExporter
 */
class RmiInvocationWrapper implements RmiInvocationHandler {

	private final Object wrappedObject;

	private final RmiBasedExporter rmiExporter;


	/**
	 * Create a new RmiInvocationWrapper for the given object
	 * @param wrappedObject the object to wrap with an RmiInvocationHandler
	 * @param rmiExporter the RMI exporter to handle the actual invocation
	 */
	public RmiInvocationWrapper(Object wrappedObject, RmiBasedExporter rmiExporter) {
		Assert.notNull(wrappedObject, "Object to wrap is required");
		Assert.notNull(rmiExporter, "RMI exporter is required");
		this.wrappedObject = wrappedObject;
		this.rmiExporter = rmiExporter;
	}


	/**
	 * Exposes the exporter's service interface, if any, as target interface.
	 * @see RmiBasedExporter#getServiceInterface()
	 */
	@Override
	public String getTargetInterfaceName() {
		Class<?> ifc = this.rmiExporter.getServiceInterface();
		return (ifc != null ? ifc.getName() : null);
	}

	/**
	 * Delegates the actual invocation handling to the RMI exporter.
	 * @see RmiBasedExporter#invoke(org.springframework.remoting.support.RemoteInvocation, Object)
	 * RMI服务激活调用
	 * 之前反复提到过，由于之前bean初始化的时候做了服务名绑定，this.registry.bind(this.serviceName,this.exportedObject)，其中
	 * exportedObject其实是被RMIInvocationWrapper进行封装的，也就是说当其他的服务调用serviceName的RMI服务时，Java会为我们封装其
	 * 内部操作，而直接会将代码转向RMIInvocationWrapper的invoke方法中
	 *
	 */
	@Override
	public Object invoke(RemoteInvocation invocation)
		throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		// 而此时的this.rmiExporter为之前初始化RMIServiceExporter，invocation为包含着需要激活的方法参数，而wrapperObject则是
		// 为之前封装代理类
		return this.rmiExporter.invoke(invocation, this.wrappedObject);
	}

}
