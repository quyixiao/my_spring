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

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * RMI exporter that exposes the specified service as RMI object with the specified name.
 * Such services can be accessed via plain RMI or via {@link RmiProxyFactoryBean}.
 * Also supports exposing any non-RMI service via RMI invokers, to be accessed via
 * {@link RmiClientInterceptor} / {@link RmiProxyFactoryBean}'s automatic detection
 * of such invokers.
 *
 * <p>With an RMI invoker, RMI communication works on the {@link RmiInvocationHandler}
 * level, needing only one stub for any service. Service interfaces do not have to
 * extend {@code java.rmi.Remote} or throw {@code java.rmi.RemoteException}
 * on all methods, but in and out parameters have to be serializable.
 *
 * <p>The major advantage of RMI, compared to Hessian and Burlap, is serialization.
 * Effectively, any serializable Java object can be transported without hassle.
 * Hessian and Burlap have their own (de-)serialization mechanisms, but are
 * HTTP-based and thus much easier to setup than RMI. Alternatively, consider
 * Spring's HTTP invoker to combine Java serialization with HTTP-based transport.
 *
 * <p>Note: RMI makes a best-effort attempt to obtain the fully qualified host name.
 * If one cannot be determined, it will fall back and use the IP address. Depending
 * on your network configuration, in some cases it will resolve the IP to the loopback
 * address. To ensure that RMI will use the host name bound to the correct network
 * interface, you should pass the {@code java.rmi.server.hostname} property to the
 * JVM that will export the registry and/or the service using the "-D" JVM argument.
 * For example: {@code -Djava.rmi.server.hostname=myserver.com}
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see RmiClientInterceptor
 * @see RmiProxyFactoryBean
 * @see Remote
 * @see RemoteException
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @see org.springframework.remoting.caucho.BurlapServiceExporter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 */
public class RmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

	private String serviceName;

	private int servicePort = 0;  // anonymous port

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private String registryHost;

	private int registryPort = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory registryClientSocketFactory;

	private RMIServerSocketFactory registryServerSocketFactory;

	private boolean alwaysCreateRegistry = false;

	private boolean replaceExistingBinding = true;

	private Remote exportedObject;

	private boolean createdRegistry = false;


	/**
	 * Set the name of the exported RMI service,
	 * i.e. {@code rmi://host:port/NAME}
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Set the port that the exported RMI service will use.
	 * <p>Default is 0 (anonymous port).
	 */
	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}

	/**
	 * Set a custom RMI client socket factory to use for exporting the service.
	 * <p>If the given object also implements {@code java.rmi.server.RMIServerSocketFactory},
	 * it will automatically be registered as server socket factory too.
	 * @see #setServerSocketFactory
	 * @see RMIClientSocketFactory
	 * @see RMIServerSocketFactory
	 * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * Set a custom RMI server socket factory to use for exporting the service.
	 * <p>Only needs to be specified when the client socket factory does not
	 * implement {@code java.rmi.server.RMIServerSocketFactory} already.
	 * @see #setClientSocketFactory
	 * @see RMIClientSocketFactory
	 * @see RMIServerSocketFactory
	 * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * Specify the RMI registry to register the exported service with.
	 * Typically used in combination with RmiRegistryFactoryBean.
	 * <p>Alternatively, you can specify all registry properties locally.
	 * This exporter will then try to locate the specified registry,
	 * automatically creating a new local one if appropriate.
	 * <p>Default is a local registry at the default port (1099),
	 * created on the fly if necessary.
	 * @see RmiRegistryFactoryBean
	 * @see #setRegistryHost
	 * @see #setRegistryPort
	 * @see #setRegistryClientSocketFactory
	 * @see #setRegistryServerSocketFactory
	 */
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	/**
	 * Set the host of the registry for the exported RMI service,
	 * i.e. {@code rmi://HOST:port/name}
	 * <p>Default is localhost.
	 */
	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	/**
	 * Set the port of the registry for the exported RMI service,
	 * i.e. {@code rmi://host:PORT/name}
	 * <p>Default is {@code Registry.REGISTRY_PORT} (1099).
	 * @see Registry#REGISTRY_PORT
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * Set a custom RMI client socket factory to use for the RMI registry.
	 * <p>If the given object also implements {@code java.rmi.server.RMIServerSocketFactory},
	 * it will automatically be registered as server socket factory too.
	 * @see #setRegistryServerSocketFactory
	 * @see RMIClientSocketFactory
	 * @see RMIServerSocketFactory
	 * @see LocateRegistry#getRegistry(String, int, RMIClientSocketFactory)
	 */
	public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
		this.registryClientSocketFactory = registryClientSocketFactory;
	}

	/**
	 * Set a custom RMI server socket factory to use for the RMI registry.
	 * <p>Only needs to be specified when the client socket factory does not
	 * implement {@code java.rmi.server.RMIServerSocketFactory} already.
	 * @see #setRegistryClientSocketFactory
	 * @see RMIClientSocketFactory
	 * @see RMIServerSocketFactory
	 * @see LocateRegistry#createRegistry(int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setRegistryServerSocketFactory(RMIServerSocketFactory registryServerSocketFactory) {
		this.registryServerSocketFactory = registryServerSocketFactory;
	}

	/**
	 * Set whether to always create the registry in-process,
	 * not attempting to locate an existing registry at the specified port.
	 * <p>Default is "false". Switch this flag to "true" in order to avoid
	 * the overhead of locating an existing registry when you always
	 * intend to create a new registry in any case.
	 */
	public void setAlwaysCreateRegistry(boolean alwaysCreateRegistry) {
		this.alwaysCreateRegistry = alwaysCreateRegistry;
	}

	/**
	 * Set whether to replace an existing binding in the RMI registry,
	 * that is, whether to simply override an existing binding with the
	 * specified service in case of a naming conflict in the registry.
	 * <p>Default is "true", assuming that an existing binding for this
	 * exporter's service name is an accidental leftover from a previous
	 * execution. Switch this to "false" to make the exporter fail in such
	 * a scenario, indicating that there was already an RMI object bound.
	 */
	public void setReplaceExistingBinding(boolean replaceExistingBinding) {
		this.replaceExistingBinding = replaceExistingBinding;
	}


	/***
	 * Java 远程方法调用，即Java RMI ，是java 编程语言里一种用于实现远程过程调用的应用程序编程接口，它使客户机上运行的程序可以调用
	 * 远程服务器上的对象，远程方法调用的特性使Java编程人员能够在网络环境中分布操作，RMI 全部的宗旨就是尽可能的简化远程接口调用
	 *
	 * Java RMI 极大的依赖于接口，在需要创建一个远程对象时，程序通过传递一个接口来隐藏底层的实现细节，客户端得到远程对象句柄正好与本地
	 * 的根代码连接，由后者负责透过网络通信，这样一来，程序员只需关心如何通过自己的接口句柄发送消息
	 *
	 * 12.1
	 * 在Spring中，同样提供了对RMI 的支持，使得在Spring下，RMI 的开发变得更加方便，同样我们还通过示例来快速的体验RMI 所提供的功能
	 * 12.1.1 使用示例
	 * 以下提供了Spring整合RMI的使用示例
	 * 建立RMI对外的接口
	 * public interface HelloRMIService{
	 *     public int getAdd(int a ,int b );
	 * }
	 *
	 * (2) 建立接口实现类
	 * public class HelloRMIServiceImpl implements HelloRMIService {
	 *     public int getAdd(int a ,int b ){
	 *         return a + b ;
	 *     }
	 * }
	 * (3)建立服务器配置文件
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
	 *		<bean id="helloRMIServiceImpl" class="test.remote.HelloRMIServiceImpl"></bean>
	 *		<bean id="myRMI" class="org.springframework.remoting.RMI.RMIServiceExporter">
	 *		 	<property name="service" ref="helloRMIServiceImpl"></property>
	 *		 	<property name="serviceName" value="helloRMI"></property>
	 *		 	<property name="serviceInterface" value="test.remote.HelloRMIService"></property>
	 *		 	<property name="registerPort" value="9999"></property>
	 *		 	<!--其他的属性自己查看，org.springframework.remoting.RMI.RMIServiceExporter的类，就知道支持的属性了-->
	 *	    </bean>
	 *
	 * </beans>
	 * (4)建立服务端测试
	 * public class ServerTest{
	 *     public static void main(String [] args){
	 *         new ClassPathXmlApplicationContext("test/remote/RMIService.xml");
	 *     }
	 * }
	 * 到这里，建立RMI服务端步骤已经结束了，服务端发布了一个两数相加的对外接口供其他的服务器调用，启动服务端测试类，其他的机器或者商品便可以
	 * 通过RMI来连接本机了
	 * (5)完成服务端配置后，还需要在测试端建立测试环境以及测试代码，首先建立测试端配置文件
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
	 *        <bean id="myClient" class="org.springframework.remoting.RMI.RMIProxyFactoryBean">
	 *        		<property name="serviceUrl" value="RMI://127.0.0.1:9999/helloRMI"></property>
	 *        		<property name="serviceInterface" value="test.remote.HelloRMIService"></property>
	 *        </bean>
	 * </beans>
	 * (6)编写测试代码
	 *  public class ClientTest{
	 *      public static void main(String [] args){
	 *          ApplicationContext context = new ClassPathXmlApplicationContext("test/remote/RMIClient.xml");
	 *          HelloRMIService hms = context.getBean("myClient",HelloRMIService.class);
	 *          System.out.println(hms.getAdd(1,2));
	 *      }
	 *  }
	 *  通过以上的步骤，实现测试端的代码调用，你会看到测试端通过RMI进行了远程连接，连接到了服务端，并使用对应的实现类HelloRMIServiceImpl
	 *  中提供了方法getAdd 来计算参数的并返回结果，你会看到控制台输出了3，当然以上的测试用例是使用同一台机器不同的商品来模似不同的机器
	 *  的RMI连接，在企业应用中，一般都是使用不同的机器来进行RMI服务的发布与访问的，你需要将接口打包，并放置在服务端的工程中
	 *  这是一个简单的方法展示 ，但是却很好的展示了Spring中使用RMI流程以及步骤，如果抛弃Spring而使用原始的RMI发布与连接，则会是一件很麻烦的
	 *  事情，有兴趣的读者可以查阅相关的资料，在Spring中使用RMI是非常简单的，Spring帮我们做了大量的工作，而这些工作都包括什么呢？
	 *  接下来，我们一起深入分析Spring中对RMI功能实现原理。
	 *  首先我们从服务端的发布功能开始着手，同样，Spring中的核心还是配置文件，这是所有功能的基础，在服务端的配置文件中我们可以看到，定义了
	 *  两个bean ，其中一个是对接口的实现类的发布，而另一个则是RMI服务的发布，使用org.springframework.remoting.RMI.RMIServiceExporter
	 *  org.springframework.remoting.RMI.RMIServiceExporter类应该是发布RMI的关键类，我们可以从此入手
	 *  根据前面展示的示例，启动Spring中的RMI服务并没有多余的操作，仅仅是开启Spring的环境，new ClassPathXmlApplicationContext ("test/remote/RMIService.xml")
	 *  仅此一句，于是，我们分析很可能是RMIServiceExporten,在初始化方法的时候做了某些操作完成了商品的发布功能，那么
	 *  这些操作的入口是这个类的哪个方法里面的呢？
	 *  进入这个类，首先要分析这个类的层次结构
	 *  根据eclipse提供的功能，我们可以查看到RMIServiceExporter 是层次结构图，那么从这个层次图中我们能得到什么信息呢？
	 *  RMIServiceExporter实现了Spring中几个比较敏感的接口，BeanClassLoaderAware,DisposableBean ,InitializingBean ,其中
	 *  DisposableBean 接口保证在实现该接口的bean 销毁时调用其destory方法，beanClassLoaderAware接口保证在实现该接口的bean 的初始化
	 *  调用其setBeanClassLoader方法，而InitializingBean接口则是保证了实现该接口的bean初始化的时候调用其afterPropertiesSet方法，
	 *  所以我们推断RMIServiceExporter的初始化函数的入口一定在其afterPropertiesSet或者setBeanClassLoader方法中，经过查看代码，确认
	 *  afterPropertiesSet为RMIServiceExporter功能初始化接口
	 */
	@Override
	public void afterPropertiesSet() throws RemoteException {
		prepare();
	}

	/**
	 * Initialize this service exporter, registering the service as RMI object.
	 * <p>Creates an RMI registry on the specified port if none exists.
	 * @throws RemoteException if service registration failed
	 */
	public void prepare() throws RemoteException {
		checkService();

		if (this.serviceName == null) {
			throw new IllegalArgumentException("Property 'serviceName' is required");
		}

		// Check socket factories for exported object.
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// Check socket factories for RMI registry.
		if (this.registryClientSocketFactory instanceof RMIServerSocketFactory) {
			this.registryServerSocketFactory = (RMIServerSocketFactory) this.registryClientSocketFactory;
		}
		if (this.registryClientSocketFactory == null && this.registryServerSocketFactory != null) {
			throw new IllegalArgumentException(
					"RMIServerSocketFactory without RMIClientSocketFactory for registry not supported");
		}

		this.createdRegistry = false;

		// Determine RMI registry to use.
		if (this.registry == null) {
			this.registry = getRegistry(this.registryHost, this.registryPort,
				this.registryClientSocketFactory, this.registryServerSocketFactory);
			this.createdRegistry = true;
		}

		// Initialize and cache exported object.
		this.exportedObject = getObjectToExport();

		if (logger.isInfoEnabled()) {
			logger.info("Binding service '" + this.serviceName + "' to RMI registry: " + this.registry);
		}

		// Export RMI object.
		if (this.clientSocketFactory != null) {
			UnicastRemoteObject.exportObject(
					this.exportedObject, this.servicePort, this.clientSocketFactory, this.serverSocketFactory);
		}
		else {
			UnicastRemoteObject.exportObject(this.exportedObject, this.servicePort);
		}

		// Bind RMI object to registry.
		try {
			if (this.replaceExistingBinding) {
				this.registry.rebind(this.serviceName, this.exportedObject);
			}
			else {
				this.registry.bind(this.serviceName, this.exportedObject);
			}
		}
		catch (AlreadyBoundException ex) {
			// Already an RMI object bound for the specified service name...
			unexportObjectSilently();
			throw new IllegalStateException(
					"Already an RMI object bound for name '"  + this.serviceName + "': " + ex.toString());
		}
		catch (RemoteException ex) {
			// Registry binding failed: let's unexport the RMI object as well.
			unexportObjectSilently();
			throw ex;
		}
	}


	/**
	 * Locate or create the RMI registry for this exporter.
	 * @param registryHost the registry host to use (if this is specified,
	 * no implicit creation of a RMI registry will happen)
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(String registryHost, int registryPort,
			RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (registryHost != null) {
			// Host explicitly specified: only lookup possible.
			if (logger.isInfoEnabled()) {
				logger.info("Looking for RMI registry at port '" + registryPort + "' of host [" + registryHost + "]");
			}
			Registry reg = LocateRegistry.getRegistry(registryHost, registryPort, clientSocketFactory);
			testRegistry(reg);
			return reg;
		}

		else {
			return getRegistry(registryPort, clientSocketFactory, serverSocketFactory);
		}
	}

	/**
	 * Locate or create the RMI registry for this exporter.
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(
			int registryPort, RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			if (this.alwaysCreateRegistry) {
				logger.info("Creating new RMI registry");
				return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
			}
			if (logger.isInfoEnabled()) {
				logger.info("Looking for RMI registry at port '" + registryPort + "', using custom socket factory");
			}
			synchronized (LocateRegistry.class) {
				try {
					// Retrieve existing registry.
					Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
					testRegistry(reg);
					return reg;
				}
				catch (RemoteException ex) {
					logger.debug("RMI registry access threw exception", ex);
					logger.info("Could not detect RMI registry - creating new one");
					// Assume no registry found -> create new one.
					return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
				}
			}
		}

		else {
			return getRegistry(registryPort);
		}
	}

	/**
	 * Locate or create the RMI registry for this exporter.
	 * @param registryPort the registry port to use
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(int registryPort) throws RemoteException {
		if (this.alwaysCreateRegistry) {
			logger.info("Creating new RMI registry");
			return LocateRegistry.createRegistry(registryPort);
		}
		if (logger.isInfoEnabled()) {
			logger.info("Looking for RMI registry at port '" + registryPort + "'");
		}
		synchronized (LocateRegistry.class) {
			try {
				// Retrieve existing registry.
				Registry reg = LocateRegistry.getRegistry(registryPort);
				testRegistry(reg);
				return reg;
			}
			catch (RemoteException ex) {
				logger.debug("RMI registry access threw exception", ex);
				logger.info("Could not detect RMI registry - creating new one");
				// Assume no registry found -> create new one.
				return LocateRegistry.createRegistry(registryPort);
			}
		}
	}

	/**
	 * Test the given RMI registry, calling some operation on it to
	 * check whether it is still active.
	 * <p>Default implementation calls {@code Registry.list()}.
	 * @param registry the RMI registry to test
	 * @throws RemoteException if thrown by registry methods
	 * @see Registry#list()
	 */
	protected void testRegistry(Registry registry) throws RemoteException {
		registry.list();
	}


	/**
	 * Unbind the RMI service from the registry on bean factory shutdown.
	 */
	@Override
	public void destroy() throws RemoteException {
		if (logger.isInfoEnabled()) {
			logger.info("Unbinding RMI service '" + this.serviceName +
					"' from registry" + (this.createdRegistry ? (" at port '" + this.registryPort + "'") : ""));
		}
		try {
			this.registry.unbind(this.serviceName);
		}
		catch (NotBoundException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("RMI service '" + this.serviceName + "' is not bound to registry"
						+ (this.createdRegistry ? (" at port '" + this.registryPort + "' anymore") : ""), ex);
			}
		}
		finally {
			unexportObjectSilently();
		}
	}

	/**
	 * Unexport the registered RMI object, logging any exception that arises.
	 */
	private void unexportObjectSilently() {
		try {
			UnicastRemoteObject.unexportObject(this.exportedObject, true);
		}
		catch (NoSuchObjectException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("RMI object for service '" + this.serviceName + "' isn't exported anymore", ex);
			}
		}
	}
}
