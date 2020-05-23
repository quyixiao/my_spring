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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adapter to use the plain {@link Controller} workflow interface with
 * the generic {@link org.springframework.web.servlet.DispatcherServlet}.
 * Supports handlers that implement the {@link LastModified} interface.
 *
 * <p>This is an SPI class, not used directly by application code.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see Controller
 * @see LastModified
 * @see HttpRequestHandlerAdapter
 */
public class SimpleControllerHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Controller);
	}

	/***
	 * HandlerInterceptor 处理
	 * Servlet API 定义的servlet过滤器可以在servlet处理每个web 请求前后分别对它进行前置处理，此外，有些时候，你可能只要处理由某些Spring MVC
	 * 处理程序处理的web请求，并在这些处理程序返回的模型属性被传递到视图之前，对它们进行一些操作
	 * 		Spring MVC 允许你通过处理拦截的Web 请求，进行前置处理和后置处理， 处理拦截是在Spring的Web 应用程序上下文中配置的，因为它们可以利用
	 * 	各种容器的特性，并引用容器声明的任何bean , 处理拦截器是针对特殊的处理程序映射进行注册的，因此它只拦截通过这些处理程序映射的请求
	 * 	的每个处理拦截都必须实现HandlerInterceptor接口，它包含三个需要你实现的回调方法，preHandler(),postHandler()和afterCompletion() ,
	 * 	第一个和第二个方法分别是在处理程序处理请求之前和之后被调用，第二个方法还允许访问返回的ModelAndView 对象，因此可以在它里面
	 * 	操作模型属性，最后一个谢谢老婆是在所有的请求处理完成之后被调用（如视图呈现之后），以下是HandlerInterceptor 的简单的实现：
	 * 	public class MyTestInterceptor implements HandlerInterceptor{
	 * 	    public boolean preHandle(HttpServletRequest request , HttpServletResponse response ,Object  handler ) throws Exception{
	 * 	        long startTime = System.currentTimeMills();
	 * 	        request.setAttribute("startTime",startTime);
	 * 	    }
	 *
	 *
	 * 	    public void postHandle(HttpServletRequest request ,HttpServletResponse response ,Object handler ,ModelAndView modelAndView ) throws Exception{
	 * 	        long startTime = (Long) request.getAttribute("startTime");
	 * 	        request.removeAttribute("startTime");
	 * 	        long endTime = System.currentMillis();
	 * 	        modelAndView.addObject("handlingTime",endTime-startTime);
	 * 	    }
	 *
	 * 	    public void afterCompletion(HttpServletRequest request ,HttpServletResponse response ,Object handler ,Exception ex ) throws Exception {
	 *
	 * 	    }
	 * 	}
	 * 	在这个拦截器的preHandler() 方法中，你记录了起始时间，并将它保存到请求属性中，这个方法应该返回true ,允许DispatcherServlet 继续处理请求
	 * 	否则，DispatcherServlet会认为这个方法已经处理了请求，直接响应返回给用户，然后在postHandler()方法中，从请求的属性中加载起始时间
	 * 	并将它当前时间进行比较，你可以计算总的持续时间，然后把这个时间添加到模型中，传递给视图，最后afterCompletion()方法无事可做，空着就可以了
	 *
	 * 	11.4.7 逻辑处理
	 * 	对于逻辑处理其实是通过适配器中转调用Handler并返回视图，对应代码：
	 * 	mv = ha.handle(processedRequest ,response ,mappedHandler.getHandler());
	 * 	同样，还是以引导示例为基础进行处理逻辑分析，之前分析过，对于普通的Web请求，Spring 默认是使用SimpleControllerHandlerAdapter 类进行处理
	 * 	,我们进行SimpleControllerHandlerAdapter 类的handle方法如下：
	 */
	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return ((Controller) handler).handleRequest(request, response);
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		if (handler instanceof LastModified) {
			return ((LastModified) handler).getLastModified(request);
		}
		return -1L;
	}

}
