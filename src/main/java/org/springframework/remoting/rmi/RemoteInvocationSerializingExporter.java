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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for remote service exporters that explicitly deserialize
 * {@link org.springframework.remoting.support.RemoteInvocation} objects and serialize
 * {@link org.springframework.remoting.support.RemoteInvocationResult} objects,
 * for example Spring's HTTP invoker.
 *
 * <p>Provides template methods for {@code ObjectInputStream} and
 * {@code ObjectOutputStream} handling.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see ObjectInputStream
 * @see ObjectOutputStream
 * @see #doReadRemoteInvocation
 * @see #doWriteRemoteInvocationResult
 */
public abstract class RemoteInvocationSerializingExporter extends RemoteInvocationBasedExporter
		implements InitializingBean {

	/**
	 * Default content type: "application/x-java-serialized-object"
	 */
	public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";


	private String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

	private boolean acceptProxyClasses = true;

	private Object proxy;


	/**
	 * Specify the content type to use for sending remote invocation responses.
	 * <p>Default is "application/x-java-serialized-object".
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	/**
	 * Return the content type to use for sending remote invocation responses.
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * Set whether to accept deserialization of proxy classes.
	 * <p>Default is "true". May be deactivated as a security measure.
	 */
	public void setAcceptProxyClasses(boolean acceptProxyClasses) {
		this.acceptProxyClasses = acceptProxyClasses;
	}

	/**
	 * Return whether to accept deserialization of proxy classes.
	 */
	public boolean isAcceptProxyClasses() {
		return this.acceptProxyClasses;
	}


	/***
	 * HttpInvoker
	 * Spring 开发小组意识到在RMI服务和基于HTTP服务如 Hessian 和Burlap 之间的空白，一方面，RMI使用Java标准的对象序列化，但是很难
	 * 穿越防火墙，但是使用自己私有的一套对象序列化机制
	 * 就这样，Spring 的HttpInvoker 就去而生，httpInvoker是一个新的远程调用模型，作为Spring 框架的一部分，来执行基于http 的远程调用
	 * 并使用java 序列化机制 ，这是让程序员高兴的事情
	 * 我们首先来看看HttpInvoker 的使用示例，httpInvoker 是基于Http的远程调用，同时也是使用Spring中提供的web服务作为基础的，所以我们测试需要
	 * 首先搭建web工程
	 * 12.2.1 使用示例
	 * （1）创建对外接口
	 * public interface HttpInvokeTestI{
	 *     public String getTestPo(String desp);
	 * }
	 *
	 * (2)创建接口的类
	 * public class HttpInvoketestImpl implements HttpInvokerTestI{
	 * @Override
	 * 	public String getTestPo(String resp){
	 * 	    return "getTestPo" + desp;
	 * 	}
	 * }
	 * (3)创建服务端配置文件 applicationContext-server.xml
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
	 *        <bean name="httpinvoketest" class="test.HttpInvokertestImpl"></bean>
	 *
	 * </beans>
	 * (4)在WEB-INF 下创建remote-servlet.xml
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
	 *        <bean name="/hit" class="org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter">
	 *        		<property name="service" ref="httpinvoketest"></property>
	 *        		<property name="serviceInterface" value="test.HttpInvoketest1"></property>
	 *        </bean>
	 *   </beans>
	 *  至此，服务端httpInvoker服务已经搭建完成，启动web 工程后就可以使用我们搭建的HttpInvoker服务了，以上的代码实现将远程传入的字符串参数处理加入
	 *  "getTestPo"前缀的功能，服务端搭建完基于Web服务的HttpInvoker后，客户端不需要使用一定的配置才能进行远程调用
	 *  (5) 创建测试端配置client.xml
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
	 *        <bean id ="remoteService" class="org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean">
	 *            <property name="serviceUrl"  value="http://localhost:8080/httpinvoketest/remoting/hit"></property>
	 *            <property name="serviceInterface" value="test.HttpInvokerTestI"></property>
	 *        </bean>
	 *  </beans>
	 *  (6)创建测试类
	 *  public class Test{
	 *      public static void main(String [] args){
	 *          ApplicationContext context = new ClassPathXmlApplicationContext("classpath:client.xml");
	 *          HttpInvokeTest1 httpInvokeTestI = (HttpInvokerTestI) context.getBean("remoteService");
	 *          System.out.println(httpInvokeTestI.getTestPo("dddd"));
	 *      }
	 *  }
	 * 运行测试类，你会看到打印出的结果
	 * getTestPo ddd
	 * dddd 是我们传入的参数，而getTestPo 则是在服务端添加的字符串，当然，上面的服务搭建与测试的过程中都是在一台机器上运行的，
	 * 如果需要在不同的机器上运行，如果需要在不同的机器上进行测试，还需要读者对服务端的相关接口打成Jar包并加入到客户端的服务器上
	 * Spring 会确保这个bean 在初始化的时候调用其afterPropertiesSet方法，而对于HttpRequestHandler接口，因为我们在配置中
	 * 已经将此接口配置成web服务，那么当有相应的请求的时候，Spring的Web服务就会在程序引导至HttpRequestHandler的handlerRequest方法中
	 * 首先，我们从afterPropertiesSet方法开始分析，看看Bean在初始化的过程中做了哪些逻辑
	 */
	@Override
	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * Initialize this service exporter.
	 */
	public void prepare() {
		this.proxy = getProxyForService();
	}

	protected final Object getProxy() {
		Assert.notNull(this.proxy, ClassUtils.getShortName(getClass()) + " has not been initialized");
		return this.proxy;
	}


	/**
	 * Create an ObjectInputStream for the given InputStream.
	 * <p>The default implementation creates a Spring {@link CodebaseAwareObjectInputStream}.
	 * @param is the InputStream to read from
	 * @return the new ObjectInputStream instance to use
	 * @throws IOException if creation of the ObjectInputStream failed
	 */
	protected ObjectInputStream createObjectInputStream(InputStream is) throws IOException {
		return new CodebaseAwareObjectInputStream(is, getBeanClassLoader(), isAcceptProxyClasses());
	}

	/**
	 * Perform the actual reading of an invocation result object from the
	 * given ObjectInputStream.
	 * <p>The default implementation simply calls
	 * {@link ObjectInputStream#readObject()}.
	 * Can be overridden for deserialization of a custom wrapper object rather
	 * than the plain invocation, for example an encryption-aware holder.
	 * @param ois the ObjectInputStream to read from
	 * @return the RemoteInvocationResult object
	 * @throws IOException in case of I/O failure
	 * @throws ClassNotFoundException if case of a transferred class not
	 * being found in the local ClassLoader
	 */
	protected RemoteInvocation doReadRemoteInvocation(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {

		Object obj = ois.readObject();
		if (!(obj instanceof RemoteInvocation)) {
			throw new RemoteException("Deserialized object needs to be assignable to type [" +
					RemoteInvocation.class.getName() + "]: " + obj);
		}
		return (RemoteInvocation) obj;
	}

	/**
	 * Create an ObjectOutputStream for the given OutputStream.
	 * <p>The default implementation creates a plain
	 * {@link ObjectOutputStream}.
	 * @param os the OutputStream to write to
	 * @return the new ObjectOutputStream instance to use
	 * @throws IOException if creation of the ObjectOutputStream failed
	 */
	protected ObjectOutputStream createObjectOutputStream(OutputStream os) throws IOException {
		return new ObjectOutputStream(os);
	}

	/**
	 * Perform the actual writing of the given invocation result object
	 * to the given ObjectOutputStream.
	 * <p>The default implementation simply calls
	 * {@link ObjectOutputStream#writeObject}.
	 * Can be overridden for serialization of a custom wrapper object rather
	 * than the plain invocation, for example an encryption-aware holder.
	 * @param result the RemoteInvocationResult object
	 * @param oos the ObjectOutputStream to write to
	 * @throws IOException if thrown by I/O methods
	 */
	protected void doWriteRemoteInvocationResult(RemoteInvocationResult result, ObjectOutputStream oos)
			throws IOException {

		oos.writeObject(result);
	}

}
