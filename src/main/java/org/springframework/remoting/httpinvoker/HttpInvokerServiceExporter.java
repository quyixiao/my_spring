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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.remoting.rmi.RemoteInvocationSerializingExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.util.NestedServletException;

/**
 * Servlet-API-based HTTP request handler that exports the specified service bean
 * as HTTP invoker service endpoint, accessible via an HTTP invoker proxy.
 *
 * <p><b>Note:</b> Spring also provides an alternative version of this exporter,
 * for Sun's JRE 1.6 HTTP server: {@link SimpleHttpInvokerServiceExporter}.
 *
 * <p>Deserializes remote invocation objects and serializes remote invocation
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
 * @see HttpInvokerClientInterceptor
 * @see HttpInvokerProxyFactoryBean
 * @see org.springframework.remoting.rmi.RmiServiceExporter
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @see org.springframework.remoting.caucho.BurlapServiceExporter
 */
public class HttpInvokerServiceExporter extends RemoteInvocationSerializingExporter
		implements HttpRequestHandler {

	/**
	 * Reads a remote invocation from the request, executes it,
	 * and writes the remote invocation result to the response.
	 * @see #readRemoteInvocation(HttpServletRequest)
	 * @see #invokeAndCreateResult(RemoteInvocation, Object)
	 * @see #writeRemoteInvocationResult(HttpServletRequest, HttpServletResponse, RemoteInvocationResult)
	 * 在HandlerRequest函数中，我们很清楚地看到了Invoker处理的大致框架，HttpInvoker服务简单说就是将请求的方法，也就是说，RemoteInvocation
	 * 对象，从客户端序列化并通过Web请求出入服务端，服务端在对传过来的序列化对象进行反序列化还原RemoteInvocation实例，然后通过
	 * 实例中的相关信息进行相关方法的调用，并将执行结果再次的返回给客户端，从handleRequest函数中，我们可以清晰的看到程序执行构架的结构
	 * (1)从request中读取序列化对象
	 * 主要是从HttpServletRequest提取相关的信息，也就是提取HttpServletRequest 中的remoteInvocation对象序列化信息以及反序列化的过程
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			// 从request 中读取序列化对象
			RemoteInvocation invocation = readRemoteInvocation(request);
			// 执行调用
			RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
			// 将结果的序列化对象写入输入流
			writeRemoteInvocationResult(request, response, result);
		}
		catch (ClassNotFoundException ex) {
			throw new NestedServletException("Class not found during deserialization", ex);
		}
	}

	/**
	 * Read a RemoteInvocation from the given HTTP request.
	 * <p>Delegates to
	 * {@link #readRemoteInvocation(javax.servlet.http.HttpServletRequest, InputStream)}
	 * with the
	 * {@link javax.servlet.ServletRequest#getInputStream() servlet request's input stream}.
	 * @param request current HTTP request
	 * @return the RemoteInvocation object
	 * @throws IOException in case of I/O failure
	 * @throws ClassNotFoundException if thrown by deserialization
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request)
			throws IOException, ClassNotFoundException {

		return readRemoteInvocation(request, request.getInputStream());
	}

	/**
	 * Deserialize a RemoteInvocation object from the given InputStream.
	 * <p>Gives {@link #decorateInputStream} a chance to decorate the stream
	 * first (for example, for custom encryption or compression). Creates a
	 * {@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream}
	 * and calls {@link #doReadRemoteInvocation} to actually read the object.
	 * <p>Can be overridden for custom serialization of the invocation.
	 * @param request current HTTP request
	 * @param is the InputStream to read from
	 * @return the RemoteInvocation object
	 * @throws IOException in case of I/O failure
	 * @throws ClassNotFoundException if thrown during deserialization
	 * 对于序列化提取也转换过程其实并没有太多的需要解释的东西，这里完全是按照标准的方式进行操作的，包括创建ObjectInputStream 以及
	 * ObjectInputStream 中提取对象的实例
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request, InputStream is)
			throws IOException, ClassNotFoundException {
		// 创建对象输入流
		ObjectInputStream ois = createObjectInputStream(decorateInputStream(request, is));
		try {
			// 从输入流中读取序列化对象
			return doReadRemoteInvocation(ois);
		}
		finally {
			ois.close();
		}
	}

	/**
	 * Return the InputStream to use for reading remote invocations,
	 * potentially decorating the given original InputStream.
	 * <p>The default implementation returns the given stream as-is.
	 * Can be overridden, for example, for custom encryption or compression.
	 * @param request current HTTP request
	 * @param is the original InputStream
	 * @return the potentially decorated InputStream
	 * @throws IOException in case of I/O failure
	 */
	protected InputStream decorateInputStream(HttpServletRequest request, InputStream is) throws IOException {
		return is;
	}

	/**
	 * Write the given RemoteInvocationResult to the given HTTP response.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param result the RemoteInvocationResult object
	 * @throws IOException in case of I/O failure
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result)
			throws IOException {

		response.setContentType(getContentType());
		writeRemoteInvocationResult(request, response, result, response.getOutputStream());
	}

	/**
	 * Serialize the given RemoteInvocation to the given OutputStream.
	 * <p>The default implementation gives {@link #decorateOutputStream} a chance
	 * to decorate the stream first (for example, for custom encryption or compression).
	 * Creates an {@link ObjectOutputStream} for the final stream and calls
	 * {@link #doWriteRemoteInvocationResult} to actually write the object.
	 * <p>Can be overridden for custom serialization of the invocation.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param result the RemoteInvocationResult object
	 * @param os the OutputStream to write to
	 * @throws IOException in case of I/O failure
	 * @see #decorateOutputStream
	 * @see #doWriteRemoteInvocationResult
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result, OutputStream os)
			throws IOException {

		ObjectOutputStream oos = createObjectOutputStream(decorateOutputStream(request, response, os));
		try {
			doWriteRemoteInvocationResult(result, oos);
		}
		finally {
			oos.close();
		}
	}

	/**
	 * Return the OutputStream to use for writing remote invocation results,
	 * potentially decorating the given original OutputStream.
	 * <p>The default implementation returns the given stream as-is.
	 * Can be overridden, for example, for custom encryption or compression.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param os the original OutputStream
	 * @return the potentially decorated OutputStream
	 * @throws IOException in case of I/O failure
	 */
	protected OutputStream decorateOutputStream(
			HttpServletRequest request, HttpServletResponse response, OutputStream os) throws IOException {

		return os;
	}

}
