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

package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;

/**
 * Interface to provide configuration for a web application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
 *
 * <p>This interface adds a {@code getServletContext()} method to the generic
 * ApplicationContext interface, and defines a well-known application attribute name
 * that the root context must be bound to in the bootstrap process.
 *
 * <p>Like generic application contexts, web application contexts are hierarchical.
 * There is a single root context per application, while each servlet in the application
 * (including a dispatcher servlet in the MVC framework) has its own child context.
 *
 * <p>In addition to standard application context lifecycle capabilities,
 * WebApplicationContext implementations need to detect {@link ServletContextAware}
 * beans and invoke the {@code setServletContext} method accordingly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 19, 2001
 * @see ServletContextAware#setServletContext
 * Spring 框架提供了构建 Web 应用程序的全功能MVC 模块，通过策略接口，Spring 框架是高度可配置的，而且支持多种视图技术
 * 例如JavaServer Pages JSP 技术，volecity 技术，Tiles ，IText 和POI ,Spring MVC 框架并不知道使用的视图，所以不会强
 * 迫你只能使用JSP 技术，Spring MVC 分离了控制器，模型对象，分派器以及处理程序对象的角色， 这种分离让它们理容易进行定制
 * Spring 的MVC 是基于Servlet功能实现的，通过实现Servlet 接口的DispatcherServlet 来封装其核心功能实现，通过将请求分派
 * 给处理程序，同时带有可配置的处理程序映射，视图解析，本地语言，主题解析以及上传文件支持，默认的处理程序是非常简单的Controller 接口
 * 只有一个方法ModelAndView handleRequest(request,response) ,Spring 提供了一个控制层次结构可以派生子类，如果应用程序需要处理
 * 用户输入表单，那么可以继承AbstractFormController ，如果需要把多项输入处理到一个表单，那么可以继承AbstractWizardFormController
 * Spring MVC 或者其他比较成熟的MVC 框架而言，解析问题无外科以下几点
 * (1) 将web 页面的请求传给服务器
 * (2) 根据不同的请求处理不同的逻辑单元
 * (3) 返回处理结果数据并跳转至响应的页面
 * 我们首先通过一个简单的示例来快速回顾Spring MVC 的使用
 *
 * 11.1 Spring 快速体验
 * （1）配置web.xml
 * 一个Web 中可以没有web.xml 文件，也就是说，web.xml 文件并不是Web 工程必需的，web.xml 文件用来初始化配置信息，比如Welcome 页面，
 * Servlet servlet-mapping ,filter ,listener 启动加载级别等，但是Spring MVC 的实现原理是通过Servlet 拦截所有的URL 来达到控制的
 * 目的的，所以web.xml的配置是必需的
 * 	<?xml version="1.0" encoding="utf-8" ?>
 * <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *          xmlns="http://java.sun.com/xml/ns/j2ee"
 *          xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
 *          version="2.4">
 *
 *     <display-name>Gupao Web Application</display-name>
 *     <!--Spring MVC 的前端控制器-->
 *     <!--当DispatcherServlet 载入后，它将从一个XML 文件中载入Spring 应用上下文，该XML 文件名字取决于<servlet-name> -->
 *     <!--这里DispatcherServlet将试图从一个叫做Springmv-servlet.xml 的文件中载入应用上下文，其默认位于WEB-INF 目录下 -->
 *     <servlet>
 *         <servlet-name>gpmvc</servlet-name>
 *         <servlet-class>com.gupaoedu.framework.webmvc.servlet.GPDispatcherServlet</servlet-class>
 *         <init-param>
 *             <!--使用ContextLoaderListener 配置时，需要告诉它Spring 配置文件中的位置 -->
 *             <param-name>contextConfigLocation</param-name>
 *             <param-value>application.properties</param-value>
 *         </init-param>
 *         <load-on-startup>1</load-on-startup>
 *     </servlet>
 *     <servlet-mapping>
 *         <servlet-name>gpmvc</servlet-name>
 *         <url-pattern>/*</url-pattern>
 *     </servlet-mapping>
 *
 *     <!--配置上下文载入器-->
 *     <!--上下文载入器载入除了DispatcherServlet载入的配置文件外的其它上下文配置文件-->
 *     <!--最常用的上下文载入器是一个Servlet监听器，其名称为ContextLoaderListener-->
 *		<listener>
 *		 	<listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
 *		</listener>
 * </web-app>
 *
 *
 *		Spring 的MVC 之所以必需配置web.xml ，其实最关键的是要配置两个地方
 *		contextConfigLocation : Spring的核心就是配置文件，可以说配置文件是Spring 中必不可少的东西，而这个参数就是使Web 与Spring
 *		的配置文件相结合的一个关键配置
 *		DispatcherServlet ： 包含了Spring MVC 请求的逻辑，Spring 使用了此类拦截Web 请求并进行相应的逻辑处理
 *
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <beans xmlns="http://www.springframework.org/schema/beans"
 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
 *        <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
 *        		<property name="prefix" value="/WEB-INF/jsp/"></property>
 *        		<property name="suffix" value=".jsp"></property>
 *        </bean>
 * </beans>
 * InternalResourceViewResolver 是一个辅助Bean ,会在ModelAndView返回的视图名前加上prefix 指定前缀，再在最后加上suffix 指定后缀，
 * 例如：由于XXController 返回的ModelAndView 中的视图名是testview ，故该视图解析器将在/WEB-INF/jsp/testview.jsp处查找视图
 * (3) 创建model
 *  模型对于Spring MVC 来说并不是必不可少的。如果处理程序非常的简单，完全可以忽略，模型创建主要的目的就是承载数据，使数据传输更加的方便
 *
 */
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * Context attribute to bind root WebApplicationContext to on successful startup.
	 * <p>Note: If the startup of the root context fails, this attribute can contain
	 * an exception or error as value. Use WebApplicationContextUtils for convenient
	 * lookup of the root WebApplicationContext.
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
	 */
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

	/**
	 * Scope identifier for request scope: "request".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_REQUEST = "request";

	/**
	 * Scope identifier for session scope: "session".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_SESSION = "session";

	/**
	 * Scope identifier for global session scope: "globalSession".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_GLOBAL_SESSION = "globalSession";

	/**
	 * Scope identifier for the global web application scope: "application".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_APPLICATION = "application";

	/**
	 * Name of the ServletContext environment bean in the factory.
	 * @see javax.servlet.ServletContext
	 */
	String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

	/**
	 * Name of the ServletContext/PortletContext init-params environment bean in the factory.
	 * <p>Note: Possibly merged with ServletConfig/PortletConfig parameters.
	 * ServletConfig parameters override ServletContext parameters of the same name.
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 * @see javax.servlet.ServletConfig#getInitParameter(String)
	 */
	String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

	/**
	 * Name of the ServletContext/PortletContext attributes environment bean in the factory.
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";


	/**
	 * Return the standard Servlet API ServletContext for this application.
	 * <p>Also available for a Portlet application, in addition to the PortletContext.
	 */
	ServletContext getServletContext();

}
