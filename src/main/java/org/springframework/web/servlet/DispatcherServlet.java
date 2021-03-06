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

package org.springframework.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * Central dispatcher for HTTP request handlers/controllers, e.g. for web UI controllers
 * or HTTP-based remote service exporters. Dispatches to registered handlers for processing
 * a web request, providing convenient mapping and exception handling facilities.
 *
 * <p>This servlet is very flexible: It can be used with just about any workflow, with the
 * installation of the appropriate adapter classes. It offers the following functionality
 * that distinguishes it from other request-driven web MVC frameworks:
 *
 * <ul>
 * <li>It is based around a JavaBeans configuration mechanism.
 *
 * <li>It can use any {@link HandlerMapping} implementation - pre-built or provided as part
 * of an application - to control the routing of requests to handler objects. Default is
 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping} and
 * {@link org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping}.
 * HandlerMapping objects can be defined as beans in the servlet's application context,
 * implementing the HandlerMapping interface, overriding the default HandlerMapping if
 * present. HandlerMappings can be given any bean name (they are tested by type).
 *
 * <li>It can use any {@link HandlerAdapter}; this allows for using any handler interface.
 * Default adapters are {@link org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter},
 * {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter}, for Spring's
 * {@link org.springframework.web.HttpRequestHandler} and
 * {@link org.springframework.web.servlet.mvc.Controller} interfaces, respectively. A default
 * {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter}
 * will be registered as well. HandlerAdapter objects can be added as beans in the
 * application context, overriding the default HandlerAdapters. Like HandlerMappings,
 * HandlerAdapters can be given any bean name (they are tested by type).
 *
 * <li>The dispatcher's exception resolution strategy can be specified via a
 * {@link HandlerExceptionResolver}, for example mapping certain exceptions to error pages.
 * Default are
 * {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver},
 * {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver}, and
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}.
 * These HandlerExceptionResolvers can be overridden through the application context.
 * HandlerExceptionResolver can be given any bean name (they are tested by type).
 *
 * <li>Its view resolution strategy can be specified via a {@link ViewResolver}
 * implementation, resolving symbolic view names into View objects. Default is
 * {@link org.springframework.web.servlet.view.InternalResourceViewResolver}.
 * ViewResolver objects can be added as beans in the application context, overriding the
 * default ViewResolver. ViewResolvers can be given any bean name (they are tested by type).
 *
 * <li>If a {@link View} or view name is not supplied by the user, then the configured
 * {@link RequestToViewNameTranslator} will translate the current request into a view name.
 * The corresponding bean name is "viewNameTranslator"; the default is
 * {@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}.
 *
 * <li>The dispatcher's strategy for resolving multipart requests is determined by a
 * {@link MultipartResolver} implementation.
 * Implementations for Apache Commons FileUpload and Servlet 3 are included; the typical
 * choice is {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}.
 * The MultipartResolver bean name is "multipartResolver"; default is none.
 *
 * <li>Its locale resolution strategy is determined by a {@link LocaleResolver}.
 * Out-of-the-box implementations work via HTTP accept header, cookie, or session.
 * The LocaleResolver bean name is "localeResolver"; default is
 * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}.
 *
 * <li>Its theme resolution strategy is determined by a {@link ThemeResolver}.
 * Implementations for a fixed theme and for cookie and session storage are included.
 * The ThemeResolver bean name is "themeResolver"; default is
 * {@link org.springframework.web.servlet.theme.FixedThemeResolver}.
 * </ul>
 *
 * <p><b>NOTE: The {@code @RequestMapping} annotation will only be processed if a
 * corresponding {@code HandlerMapping} (for type-level annotations) and/or
 * {@code HandlerAdapter} (for method-level annotations) is present in the dispatcher.</b>
 * This is the case by default. However, if you are defining custom {@code HandlerMappings}
 * or {@code HandlerAdapters}, then you need to make sure that a corresponding custom
 * {@code DefaultAnnotationHandlerMapping} and/or {@code AnnotationMethodHandlerAdapter}
 * is defined as well - provided that you intend to use {@code @RequestMapping}.
 *
 * <p><b>A web application can define any number of DispatcherServlets.</b>
 * Each servlet will operate in its own namespace, loading its own application context
 * with mappings, handlers, etc. Only the root application context as loaded by
 * {@link org.springframework.web.context.ContextLoaderListener}, if any, will be shared.
 *
 * <p>As of Spring 3.1, {@code DispatcherServlet} may now be injected with a web
 * application context, rather than creating its own internally. This is useful in Servlet
 * 3.0+ environments, which support programmatic registration of servlet instances.
 * See the {@link #DispatcherServlet(WebApplicationContext)} javadoc for details.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.context.ContextLoaderListener
 * 在Spring 中，ContextLoaderListener 只是辅助功能，用于创建webApplicationContext 类型的实例，而真正的逻辑实现其实是在
 * DispatcherServlet 中进行的，DispatcherServlet是实现servlet接口的实现类，servlet是一个java 编写的程序，此程序是基于
 * HTTP协义的，在服务器端口运行的，如tomcat ，是按照servlet规范编写的一个java 类，主要是处理客户端请求并将其结果发送到客户端，
 * servlet 的生命周期是由servlet 的容器来控制，它可以分为3个阶段，初始化，运行和销毁
 * （1）servlet容器加载servlet 类，把servlet类的.class文件中的数据读到内存中
 *      servlet创建一个ServletConfig 对象，ServletConfig 对象包含了servlet的初始化配置信息
 *      servlet容器创建了一个servlet对象
 *      servlet容器调用servlet对象的init方法进行初始化
 * （2）运行阶段
 * 	当servlet 容器接收到一个请求时，servlet容器会针对这个请求创建了servletRequest 和servletResponse对象，然后调用service方法
 * 	并将这个两个参数传递给了service方法，。service方法通过servletRequest 对象获得请求的信息，并处理该请求，再通过servletResponse
 * 	对象生成这个请求的响应结果，然后销毁servletRequest 和servletResponse对象，我们不管这个请求是post还是get 提交的，最终这个请求
 * 	都是由service 方法来处理的
 * 	（3） 销毁方法
 * 	当web 应用被终止时，servlet容器会先调用这个servlet 对象的destory方法，然后再销毁servlet 对象，同时也会销毁与servlet 对象相关的
 * 	联的servletConfig 对象，我们可以在destory方法的实现中，释放servlet所占用的资源，如关闭数据库连接，关闭文件输入，输出流等
 * 	servlet的构架是由两个java 包组成的，javax.servlet和javax.servlet.http，在javax.servlet包中定义了所有的servlet类都必需实现或
 * 	扩展的通用接口和类，在javax.servlet.http 包中定义了采用HTTP 通信协义的HttpServlet类
 * 	servlet 被设计成请求驱动，servlet的请求可能包含多个数据项，当Web 容器接收到某个servlet请求时，servlet把请求封装成一个HttpServletRequest
 * 	对象，然后把对象传给servlet的对应的服务方法
 * 	HTTP 的请求方式包括delete,get,options,post,put和trace，在HttpServlet类中分别提供了相应的服务方法，它们是doDelete(),doGet(),
 * 	doOptions(),doPost(),doPut()和doTrace()
 *
 *
 *
 *
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

	/** Well-known name for the MultipartResolver object in the bean factory for this namespace. */
	public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

	/** Well-known name for the LocaleResolver object in the bean factory for this namespace. */
	public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

	/** Well-known name for the ThemeResolver object in the bean factory for this namespace. */
	public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

	/**
	 * Well-known name for the HandlerMapping object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerMappings" is turned off.
	 * @see #setDetectAllHandlerMappings
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

	/**
	 * Well-known name for the HandlerAdapter object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerAdapters" is turned off.
	 * @see #setDetectAllHandlerAdapters
	 */
	public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

	/**
	 * Well-known name for the HandlerExceptionResolver object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerExceptionResolvers" is turned off.
	 * @see #setDetectAllHandlerExceptionResolvers
	 */
	public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

	/**
	 * Well-known name for the RequestToViewNameTranslator object in the bean factory for this namespace.
	 */
	public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

	/**
	 * Well-known name for the ViewResolver object in the bean factory for this namespace.
	 * Only used when "detectAllViewResolvers" is turned off.
	 * @see #setDetectAllViewResolvers
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

	/**
	 * Well-known name for the FlashMapManager object in the bean factory for this namespace.
	 */
	public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

	/**
	 * Request attribute to hold the current web application context.
	 * Otherwise only the global web app context is obtainable by tags etc.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#findWebApplicationContext
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

	/**
	 * Request attribute to hold the current LocaleResolver, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocaleResolver
	 */
	public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

	/**
	 * Request attribute to hold the current ThemeResolver, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeResolver
	 */
	public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

	/**
	 * Request attribute to hold the current ThemeSource, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeSource
	 */
	public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

	/**
	 * Name of request attribute that holds a read-only {@code Map<String,?>}
	 * with "input" flash attributes saved by a previous request, if any.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the "output" {@link FlashMap} with
	 * attributes to save for a subsequent request.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the {@link FlashMapManager}.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getFlashMapManager(HttpServletRequest)
	 */
	public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

	/**
	 * Name of request attribute that exposes an Exception resolved with an
	 * {@link HandlerExceptionResolver} but where no view was rendered
	 * (e.g. setting the status code).
	 */
	public static final String EXCEPTION_ATTRIBUTE = DispatcherServlet.class.getName() + ".EXCEPTION";

	/** Log category to use when no mapped handler is found for a request. */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * Name of the class path resource (relative to the DispatcherServlet class)
	 * that defines DispatcherServlet's default strategy names.
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";


	/** Additional logger to use when no mapped handler is found for a request. */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	private static final Properties defaultStrategies;

	static {
		// Load default strategy implementations from properties file.
		// This is currently strictly internal and not meant to be customized
		// by application developers.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'DispatcherServlet.properties': " + ex.getMessage());
		}
	}

	/** Detect all HandlerMappings or just expect "handlerMapping" bean? */
	private boolean detectAllHandlerMappings = true;

	/** Detect all HandlerAdapters or just expect "handlerAdapter" bean? */
	private boolean detectAllHandlerAdapters = true;

	/** Detect all HandlerExceptionResolvers or just expect "handlerExceptionResolver" bean? */
	private boolean detectAllHandlerExceptionResolvers = true;

	/** Detect all ViewResolvers or just expect "viewResolver" bean? */
	private boolean detectAllViewResolvers = true;

	/** Throw a NoHandlerFoundException if no Handler was found to process this request? **/
	private boolean throwExceptionIfNoHandlerFound = false;

	/** Perform cleanup of request attributes after include request? */
	private boolean cleanupAfterInclude = true;

	/** MultipartResolver used by this servlet */
	private MultipartResolver multipartResolver;

	/** LocaleResolver used by this servlet */
	private LocaleResolver localeResolver;

	/** ThemeResolver used by this servlet */
	private ThemeResolver themeResolver;

	/** List of HandlerMappings used by this servlet */
	private List<HandlerMapping> handlerMappings;

	/** List of HandlerAdapters used by this servlet */
	private List<HandlerAdapter> handlerAdapters;

	/** List of HandlerExceptionResolvers used by this servlet */
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	/** RequestToViewNameTranslator used by this servlet */
	private RequestToViewNameTranslator viewNameTranslator;

	/** FlashMapManager used by this servlet */
	private FlashMapManager flashMapManager;

	/** List of ViewResolvers used by this servlet */
	private List<ViewResolver> viewResolvers;

	/**
	 * Create a new {@code DispatcherServlet} that will create its own internal web
	 * application context based on defaults and values provided through servlet
	 * init-params. Typically used in Servlet 2.5 or earlier environments, where the only
	 * option for servlet registration is through {@code web.xml} which requires the use
	 * of a no-arg constructor.
	 * <p>Calling {@link #setContextConfigLocation} (init-param 'contextConfigLocation')
	 * will dictate which XML files will be loaded by the
	 * {@linkplain #DEFAULT_CONTEXT_CLASS default XmlWebApplicationContext}
	 * <p>Calling {@link #setContextClass} (init-param 'contextClass') overrides the
	 * default {@code XmlWebApplicationContext} and allows for specifying an alternative class,
	 * such as {@code AnnotationConfigWebApplicationContext}.
	 * <p>Calling {@link #setContextInitializerClasses} (init-param 'contextInitializerClasses')
	 * indicates which {@code ApplicationContextInitializer} classes should be used to
	 * further configure the internal application context prior to refresh().
	 * @see #DispatcherServlet(WebApplicationContext)
	 */
	public DispatcherServlet() {
		super();
	}

	/**
	 * Create a new {@code DispatcherServlet} with the given web application context. This
	 * constructor is useful in Servlet 3.0+ environments where instance-based registration
	 * of servlets is possible through the {@link ServletContext#addServlet} API.
	 * <p>Using this constructor indicates that the following properties / init-params
	 * will be ignored:
	 * <ul>
	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
	 * </ul>
	 * <p>The given web application context may or may not yet be {@linkplain
	 * ConfigurableApplicationContext#refresh() refreshed}. If it has <strong>not</strong>
	 * already been refreshed (the recommended approach), then the following will occur:
	 * <ul>
	 * <li>If the given context does not already have a {@linkplain
	 * ConfigurableApplicationContext#setParent parent}, the root application context
	 * will be set as the parent.</li>
	 * <li>If the given context has not already been assigned an {@linkplain
	 * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
	 * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
	 * the application context</li>
	 * <li>{@link #postProcessWebApplicationContext} will be called</li>
	 * <li>Any {@code ApplicationContextInitializer}s specified through the
	 * "contextInitializerClasses" init-param or through the {@link
	 * #setContextInitializers} property will be applied.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called if the
	 * context implements {@link ConfigurableApplicationContext}</li>
	 * </ul>
	 * If the context has already been refreshed, none of the above will occur, under the
	 * assumption that the user has performed these actions (or not) per their specific
	 * needs.
	 * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
	 * @param webApplicationContext the context to use
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public DispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
	}

	/**
	 * Set whether to detect all HandlerMapping beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerMapping" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerMapping, despite multiple HandlerMapping beans being defined in the context.
	 */
	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
		this.detectAllHandlerMappings = detectAllHandlerMappings;
	}

	/**
	 * Set whether to detect all HandlerAdapter beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerAdapter" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerAdapter, despite multiple HandlerAdapter beans being defined in the context.
	 */
	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
	}

	/**
	 * Set whether to detect all HandlerExceptionResolver beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerExceptionResolver" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerExceptionResolver, despite multiple HandlerExceptionResolver beans being defined in the context.
	 */
	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
	}

	/**
	 * Set whether to detect all ViewResolver beans in this servlet's context. Otherwise,
	 * just a single bean with name "viewResolver" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * ViewResolver, despite multiple ViewResolver beans being defined in the context.
	 */
	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
		this.detectAllViewResolvers = detectAllViewResolvers;
	}

	/**
	 * Set whether to throw a NoHandlerFoundException when no Handler was found for this request.
	 * This exception can then be caught with a HandlerExceptionResolver or an
	 * {@code @ExceptionHandler} controller method.
	 * <p>Note that if {@link org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler}
	 * is used, then requests will always be forwarded to the default servlet and a
	 * NoHandlerFoundException would never be thrown in that case.
	 * <p>Default is "false", meaning the DispatcherServlet sends a NOT_FOUND error through the
	 * Servlet response.
	 * @since 4.0
	 */
	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
	}

	/**
	 * Set whether to perform cleanup of request attributes after an include request, that is,
	 * whether to reset the original state of all request attributes after the DispatcherServlet
	 * has processed within an include request. Otherwise, just the DispatcherServlet's own
	 * request attributes will be reset, but not model attributes for JSPs or special attributes
	 * set by views (for example, JSTL's).
	 * <p>Default is "true", which is strongly recommended. Views should not rely on request attributes
	 * having been set by (dynamic) includes. This allows JSP views rendered by an included controller
	 * to use any model attributes, even with the same names as in the main JSP, without causing side
	 * effects. Only turn this off for special needs, for example to deliberately allow main JSPs to
	 * access attributes from JSP views rendered by an included controller.
	 */
	public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
		this.cleanupAfterInclude = cleanupAfterInclude;
	}

	/**
	 * This implementation calls {@link #initStrategies}.
	 */
	@Override
	protected void
	onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * Initialize the strategy objects that this servlet uses.
	 * <p>May be overridden in subclasses in order to initialize further strategy objects.
	 * 初始化策略
	 * 到这里就完成了Spring MVC 的九大组件的初始化，接下来，我们来看URL 和Controller的关系是如何建立的，HandlerMaping 的子类
	 * AbstractDetectingUrlHandlerMapping ，实现了InitApplicationContext()方法，我们直接看到子类的初始化容器方法
	 *
	 */
	protected void initStrategies(ApplicationContext context) {
		// 多文件上传的组件
		// (1)初始化MultipartResolver
		// 在Spring 中，MultipartResolver主要用来处理文件上传，默认的情况下，Spring 是没有multipart处理的，因为一些开发都想
		// 要自己处理他们，如果想使用Spring 的multipart，则需要在Web 应用的上下文中添加multipart解析器，这样每个请求就会被检测是否
		// 包含了multipart，然而，如果请求中包含了multipart，那么上下文中定义的MultipartResolver就会解析它，这样的请求中的multipart
		// 属性就会像其他的属性一样被处理，常用的配置如下：
		// <bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
		// 		<!--该属性用来配置可上传文件的最大byte数-->
		// 		<property name="maxMumFileSize" />
		// </bean>
		// 当然，CommonsMultipartResolver 还提供了其他的功能用于帮助用户完成上传功能，有兴趣的读者可以进一步的查看
		// 那么MultipartResolver就是在initMultipartResolver中被加入了DispatcherServlet 中的
		/**
		 * private void initMultipartResolver(ApplicationContext context) {
		 * 		try {
		 * 			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
		 * 			if (logger.isDebugEnabled()) {
		 * 				logger.debug("Using MultipartResolver [" + this.multipartResolver + "]");
		 *                        }* 		}
		 * 		catch (NoSuchBeanDefinitionException ex) {
		 * 			// Default is no multipart resolver.
		 * 			this.multipartResolver = null;
		 * 			if (logger.isDebugEnabled()) {
		 * 				logger.debug("Unable to locate MultipartResolver with name '" + MULTIPART_RESOLVER_BEAN_NAME +
		 * 						"': no multipart request handling provided");
		 *            }
		 *        }
		 * }
		 * 因为之前的步骤已经完成了Spring 中配置文件的解析，所以在这里只要在配置文件中注册过都可以通过ApplicationContext 提供的
		 * getBean方法来直接获取对应的bean ，进而初始化MultipartResolver中的multipartResolver变量
		 *
		 */
		//
		initMultipartResolver(context);
		// 初始化本地语言环境
		// 初始化LocaleResolver
		// 在Spring 的国际化配置中一共有3种使用方式
		// 基于URL 参数的配置
		// 通过URL 参数控制国际化，比如你的页面上加句<a href="?locale=zh_CN"> 简体中文 </a> 来控制项目中使用的国际化参数，而提供的
		/***
		 * 的功能就是AcceptHeaderLocaleResolver，默认的参数名locale ，注意大小写，里面放的就是你提交的参数，比如 en_US,zh_CN 之类的
		 * 具体的配置如下：
		 * <bean id="localeResolver" class="org.springframework.web.servlet.ill8n.AccptHeader.LocaleResolver"></bean>
		 * 基于session 的配置
		 * 它通过检验用户会话中的预置的属性来解析区域，最常用的是根据用户本次会话过程中的语言设定决定语言类，例如 用户登录时选择语言
		 * accept-language HTTP 头部确定的默认区域
		 * <bean id="localeResolver" class="org.springframework.web.servlet.il8n.SessionLocaleResolver"></bean>
		 * 基于Cookie的国际化配置
		 * CookieLocaleResolver用于通过浏览器cookie设置取得Locale对象，这种策略在应用程序不支持会话或者状态必需保存的客户端时有用
		 * ,配置如下：
		 * 	<bean id="localeResolver" class="org.springframework.web.servlet.il8n.CookieLocaleResolver"></bean>
		 * 	这3种方式都可以解析国际化问题，但是对于LocalResolver 的使用基础是在DispatcherServlet初始化 localeResolver
		 * 提取配置文件中设置的LocaleResolver 来初始化DispatcherServlet 中的localeResolver属性
		 */
		initLocaleResolver(context);
		// 初始化模板处理器
		/**
		 * (3)初始化ThemeResolver
		 * 在Web 开发中经常会遇到通过主题Theme 来控制网页风格，这将进一步改善用户体验，简单地说，一个主题是一组静态的资源，比如样式表和图片
		 * ,它们可以影响应用程序的视觉效果，Spring 中的主题功能和国际化功能非常的相似，构成Spring 主题功能的主要包括如下内容：
		 * 主题资源
		 * org.springframework.ui.context.ThemeSource 是Spring 中主题资源接口，Spring 主题需要通过 ThemeSource 接口来实现
		 * 存放国际化功能非常的类似，构成Spring主题主要包括如下内容
		 * 主题资源
		 * org.springframework.ui.context.support.ResourceBundleThemeSource 是themeSource 接口的默认实现类，也就是通过
		 * ResourceBundle资源的方式定义主题，在Spring 中配置如下
		 * <bean id="themeSource" class="org.springframework.ui.context.support.ResourceBundleThemeSource">
		 * 		<property name="basenamePrefix" value="com.test."></property>
		 * </bean>
		 * 默认状态下是在类路径目录下查找相应的资源文件，也可以通过basenamePrefix 来制定这样，DispatcherServlet就是在com.test包下查找资源文件
		 * 主题解析器
		 * ThemeSource 定义了一些主题资源，那么不同的用户使用什么样的主题资源呢？ 这又是由谁来定义的呢？ org.springframework.web.servlet.ThemeResolver
		 * 是主题解析器接口，主题解析的工作便是由它的子类来完成的。
		 * 对于主题解析器的子类主要有3个比较常用的实现，以主题文件summer.properties为例
		 * <bean id="themeResolver" class="org.springframework.web.servlet.theme.FixedThemeResolver">
		 * 		<property name="defaultThemeName" value="summer"></property>
		 * </bean>
		 * 以上的配置的作用就是设置主题文件为summer.properties ，在整个项目内固定不变
		 * CookieThemeResolver 用于实现用户所选的主题，以cookie的形式存放在客户端机器上，配置如下：
		 * <bean id="themeResolver" class="org.springframework.web.servlet.theme.CookieThemeResolver">
		 * 		<property name="defaultThemeName" value="summper"></property>
		 * </bean>
		 *SessionThemeResolver 用于保存在用户的HTTP Session 中
		 * <bean id="themeResolver" class="org.springframework.web.servlet.theme.SessionThemeResolver">
		 * 		<property name="defaultThemeName" value="summer"></property>
		 * </bean>
		 * AbstractThemeResolver 是一个抽象的类被SessionThemeResolver 和FixedThemeResolver 继承，用户也可以继承它来自定义主题解析器
		 * <bean id="themeChangeInterceptor" class="org.springframework.web.servlet.theme.ThemeChangeInterceptor">
		 * 		<property name="paramName" value="themeName"></property>
		 * </bean>
		 * 其中设置了用户请求参数名themeName,即URL 为?themeName = 具体的主题名称，此外，还需要在handlerMapping 配置主题拦截器，
		 * 当然需要在HandlerMapping 中添加拦截器
		 * <property name="interceptors">
		 * 		<list>
		 * 		 	<ref local="themeChangeInterceptor"/>
		 * 		</list>
		 * </property>
		 * 了解了主题文件的简单使用之后，再来查看解析器的初始化工作，与其他的变量初始化工作相同，设计师文件解析器初始化并没有任何特别需要说明的地方
		 */
		initThemeResolver(context);
		// 初始化handlerMapping
		// (3) 初始化HandlerMappings
		//  当客户端发出Request时DispatcherServlet 会将Request提交给HandlerMapping ，然后HandlerMapping 根据WebApplicationContext
		//的配置来回传给DispatcherServlet相应的Controller
		// 在基于Spring MVC 的Web 应用程序中，我们可以为DispatcherServlet提供了多个HandlerMapping 供其使用，DispatcherServlet 在选用
		// HandlerMapping 的过程中，将根据我们所指定的一系列的HandlerMapping 的优先级进行排序，然后优先的在前面的HandlerMapping ,如果当前
		// HandlerMapping 能够返回可用的Handler ，DispatcherServlet 则使用当前返回的Handler 进行web请求的处理，而不再继续询问其他的
		// HandlerMapping ，否则，DispathcherServlet 将继续按照各个HandlerMapping 的优先级进行询问，直到获取一个可用的Handler为止
		// 初始化配置如下：
		// ... handlerMapping
		// 默认情况下，Spring MVC 将加载当前系统中所有实现了HandlerMapping 接口的 Bean ,如果只期望SpringMVC 加载指定的handlerMapping 时，
		// 可以修改web.xml 中的dispatcherServlet 的初始化参数，将detectAllHandlerMapping 的值设置为false :
		//  <init-param>
		// 		<param-name>detectAllHandlerMapping</param-name>
		//		<param-value>false</param-value>
		//  </init-param>
		// 此时，Spring MVC 将查找名为"handlerMapping" 的bean ,并作用当前系统中唯一的handlerMapping ，如果没有定义handlerMapping的话，
		// 则Spring MVC 将按照org.springframework.web.servlet.DispatcherServlet 所在的目录下DispatcherServlet.properties ，
		// 中所定义的org.springframework.web.servlet.HandlerMapping 的内容来加载默认的handlerMapping （用户没有自定义Strategies的情况下）
		initHandlerMappings(context);
		// 初始化参数适配器
		// (5) 初始化HandlerAdapters
		// 你名字也能联想到这是一个典型的适配器模式的使用，在计算机编程中，适配器模式将这个类的接口适配成用户所期待的，使用适配器，可以使用
		// 接口在不兼容而无法在一起工作的类协同工作，做法是将类自己的接口包裹在一个已经存在的类中，那么处理Handler 时为什么会使用适配器
		// 模式呢？回答这个问题我们首先分析他的初始化逻辑
		initHandlerAdapters(context);
		// 初始化异常拦截器
		// (6) 初始化HandlerExceptionResolvers
		// 基于HnalderExceptionResolver 接口的异常处理，使用这种方式只需要实现resolverException 方法，该方法返回了一个modelAndView对象
		// 在方法的内部对异常的类型进行判断，然后尝试生成对象的ModelAndView 对象，如果该方法返回了null,则Spring 会继续寻找其他的实现了
		// HandlerExceptionResolver 接口的bean ,换名话说，Spring 会搜索所有的注册在其环境中实现了的HandlerExceptionResolver
		// 接口的bean ,逐个执行，直接返回了一个ModelAndView 对象
		// @Component
		// public class ExceptionHandler implements HandlerExceptionResolver {
		//		private static final Log log = LogFactory.getLog(ExceptionHandler.class);
		// 		@Override
		//		public ModelAndView resolverException(HttpServletRequest request ,HttpServletResponse response ,Object obj ,Exception exception){
		//			request.setAttribute("exception",exception.toString());
		//			request.setAttribute("exceptionStack",exception);
		//			logs.error(exception.toString(),exception);
		//			return new ModelAndView("error/exception");
		//		}
		// }
		// 这个类必需声明到Spring 中去，让Spring 管理它，在Spring 的配置文件中applicationContext.xml 中增加以下的内容
		// <bean id="exceptionHandler" class="com.test.exception.MyExceptionHandler">
		//
		initHandlerExceptionResolvers(context);
		// 初始化视图预处理器
		// (7) 初始化RequestToViewNameTranslator
		// 当Controller 处理器方法没有返回一个View的对象或者逻辑视图名称，并且在该方法中没有直接往response 的输出流里写数据的时候，
		// Spring 就会采用约定好的方式提供一个逻辑视图名称，这个逻辑视图名称是通过Spring 定义的org.springframework.web.servlet.RequestToViewNameTranslator
		// 接口的getViewName 方法来实现的，我们可以实现自己的RequestToViewNameTranslator 接口来约定好没有返回的视图名称
		// 的时候如何确定视图名称，Spring 已经给我们提供了一个它自己的实现，那就是org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator
		// 在介绍DefaultRequestToViewNameTranslator是如何约定视图名称之前，先来看一下它支持用户定义的属性
		// prefix : 前缀，表示约定好的视图名称需要加上前缀，默认是空串
		// suffix : 后缀，表示约定好的视图名称需要加上的后缀，默认是空串
		// separator : 分隔符，默认是斜杠 "/"
		// stripLeadingSlash : 如果首字符分隔符，是否要去除，默认是true
		// stripTailingSlash : 如果最后一个字符是分隔符，是否要去除，默认是true
		// stripExtension : 如果请求路径包含扩展名是否要去除，默认是true
		// urlDecode : 是否需要对URL 解码，默认是true ,它会采用request 指定的编码或者ISO-8859-1 编码URL进行解码
		// 当我们没有在Spring MVC 的配置文件中手动定义一个名为viewNameTranslator 的bean 的时候，Spring 就会为我们提供一个默认的
		// viewNameTranslator ,即DefaultRequestToViewNameTranslator ，
		// 接下来看一下，当Controller 处理器方法没有返回逻辑视图名称时，DefaultRequestToViewNameTranslator 是如何约定视图名称的，
		// 然后根据提供的属性做一些改造，把发广告后的结果作为视图名称返回，这里请求的路径是http://localhost/app/test/index.html 为例
		// 来说明一下DefaultRequestToViewNameTranslator 是如何工作的，请求的路径对应的请求URI 为/test/index.html，我们来看以下的几种情况，它分别对应
		// 的逻辑视图的名称是什么
		// prefix 和suffix 如果都存在，其他的默认值，那么对应的返回的逻辑视图名称应该是prefixtest/indexsuffix
		// stripLeadingSlash 和stripExtension 都为false ，其他默认，这时候对应的逻辑视图名称/product/index.html
		// 都采用的是默认值配置时，返回的逻辑视图名称应该是product/index
		// 如果逻辑视图的名称跟请求的路径相同或者相关关系都是一样的，那么我们就可以采用Spring 为我们事先约定好的逻辑视图名称返回
		// ，这可以大大的简化我们的开发工作，而以上的功能实现关系属性viewNameTranslator ，则是在initRequestToViewNameTranslator 来完成
		initRequestToViewNameTranslator(context);
		// 初始化视图转换器
		// 初始化ViewResolves
		// 在Spring MVC 中，当Controller 将请求处理结果放入到ModelAndView 中以后DispatcherServlet 会根据ModelAndView选择合适的
		// 视图进行渲染，那么在Spring MVC 中是如何选择合适的View 的呢？View 对象是如何创建的呢？ 答案就是ViewResolver中，ViewResolver
		// 接口定义了resolverViewName 方法，根据viewName 创建合适的类型View实现
		// 那么如何配置ViewResolver 的呢？ 在Spring 中，ViewResolver 作为Spring Bean 的存在，可以在Spring 配置文件中进行配置
		// 例如下面的代码，配置了JSP 相关的ViewResolver
		/***
		 * <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
		 * 		<property name="prefix" value="/WEB-INF/views/"></property>
		 * 		<property name="suffix" view=".jsp"></property>
		 * </bean>
		 * viewResolvers 属性初始化工作在initViewResolvers 中完成
		 *
		 */
		initViewResolvers(context);
		// 初始化 FlashMap管理器
		/**
		 * (9) 初始化FlashMapManager
		 * Spring MVC Flash attributes 提供了一个请求存在属性，可供其他的请求使用，在使用重定向时候非常必要，例如Post/Redirect/Get
		 * 模式，Flash attributes 在重定向之前暂存（就像存在session 中） 以便重定向之后还能使用，并立即删除
		 * Spring MVC 有两个主要的抽象来支持flash attributes ，FlashMap 用于保存flash attributes ，在FlashMapManager 用于存储
		 * 检索，管理flashMap 实例
		 * flash attribute 支持默认的开启（"on"） 并不需要显示的启用，它永远不会导致HTTP Session 的创建，这两个FlashMap 实例都可以通过
		 * 静态方法RequestContextUtils 从Spring MVC 的任意位置访问
		 * flashMapManager 的初始化在initFlashMapManager 中完成
		 */
		initFlashMapManager(context);
	}


	/**
	 * Initialize the MultipartResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * no multipart handling is provided.
	 */
	private void initMultipartResolver(ApplicationContext context) {
		try {
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver [" + this.multipartResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MultipartResolver with name '" + MULTIPART_RESOLVER_BEAN_NAME +
						"': no multipart request handling provided");
			}
		}
	}

	/**
	 * Initialize the LocaleResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * we default to AcceptHeaderLocaleResolver.
	 */
	private void initLocaleResolver(ApplicationContext context) {
		try {
			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using LocaleResolver [" + this.localeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate LocaleResolver with name '" + LOCALE_RESOLVER_BEAN_NAME +
						"': using default [" + this.localeResolver + "]");
			}
		}
	}

	/**
	 * Initialize the ThemeResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * we default to a FixedThemeResolver.
	 */
	private void initThemeResolver(ApplicationContext context) {
		try {
			this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using ThemeResolver [" + this.themeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Unable to locate ThemeResolver with name '" + THEME_RESOLVER_BEAN_NAME + "': using default [" +
								this.themeResolver + "]");
			}
		}
	}

	/**
	 * Initialize the HandlerMappings used by this class.
	 * <p>If no HandlerMapping beans are defined in the BeanFactory for this namespace,
	 * we default to BeanNameUrlHandlerMapping.
	 */
	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		if (this.detectAllHandlerMappings) {
			// Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
				// We keep HandlerMappings in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerMappings);
			}
		}
		else {
			try {
				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
				this.handlerMappings = Collections.singletonList(hm);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerMapping later.
			}
		}

		// Ensure we have at least one HandlerMapping, by registering
		// a default HandlerMapping if no other mappings are found.
		if (this.handlerMappings == null) {
			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerMappings found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the HandlerAdapters used by this class.
	 * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,
	 * we default to SimpleControllerHandlerAdapter.
	 */
	private void initHandlerAdapters(ApplicationContext context) {
		this.handlerAdapters = null;

		if (this.detectAllHandlerAdapters) {
			// Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerAdapter> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<HandlerAdapter>(matchingBeans.values());
				// We keep HandlerAdapters in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerAdapters);
			}
		}
		else {
			try {
				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
				this.handlerAdapters = Collections.singletonList(ha);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerAdapter later.
			}
		}

		// Ensure we have at least some HandlerAdapters, by registering
		// default HandlerAdapters if no other adapters are found.
		if (this.handlerAdapters == null) {
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerAdapters found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the HandlerExceptionResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * we default to no exception resolver.
	 */
	private void initHandlerExceptionResolvers(ApplicationContext context) {
		this.handlerExceptionResolvers = null;

		if (this.detectAllHandlerExceptionResolvers) {
			// Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<HandlerExceptionResolver>(matchingBeans.values());
				// We keep HandlerExceptionResolvers in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
			}
		}
		else {
			try {
				HandlerExceptionResolver her =
						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				this.handlerExceptionResolvers = Collections.singletonList(her);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, no HandlerExceptionResolver is fine too.
			}
		}

		// Ensure we have at least some HandlerExceptionResolvers, by registering
		// default HandlerExceptionResolvers if no other resolvers are found.
		if (this.handlerExceptionResolvers == null) {
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerExceptionResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the RequestToViewNameTranslator used by this servlet instance.
	 * <p>If no implementation is configured then we default to DefaultRequestToViewNameTranslator.
	 * 如果逻辑视图名称跟请求的路径相同或者相关关系都是一样的，那么我们就采用 Spring 为我们事先约定好的逻辑视图名称返回，这可以大大的简化
	 * 我们的开发工作，而以上的功能实现的关键属性 viewNameTranslator ，则是在 initRequestToViewNameTranslator
	 */
	private void initRequestToViewNameTranslator(ApplicationContext context) {
		try {
			// viewNameTranslator
			this.viewNameTranslator =
					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using RequestToViewNameTranslator [" + this.viewNameTranslator + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate RequestToViewNameTranslator with name '" +
						REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME + "': using default [" + this.viewNameTranslator +
						"]");
			}
		}
	}

	/**
	 * Initialize the ViewResolvers used by this class.
	 * <p>If no ViewResolver beans are defined in the BeanFactory for this
	 * namespace, we default to InternalResourceViewResolver.
	 */
	private void initViewResolvers(ApplicationContext context) {
		this.viewResolvers = null;

		if (this.detectAllViewResolvers) {
			// Find all ViewResolvers in the ApplicationContext, including ancestor contexts.
			Map<String, ViewResolver> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.values());
				// We keep ViewResolvers in sorted order.
				AnnotationAwareOrderComparator.sort(this.viewResolvers);
			}
		}
		else {
			try {
				// viewResolver
				ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
				this.viewResolvers = Collections.singletonList(vr);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default ViewResolver later.
			}
		}

		// Ensure we have at least one ViewResolver, by registering
		// a default ViewResolver if no other resolvers are found.
		if (this.viewResolvers == null) {
			this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No ViewResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the {@link FlashMapManager} used by this servlet instance.
	 * <p>If no implementation is configured then we default to
	 * {@code org.springframework.web.servlet.support.DefaultFlashMapManager}.
	 * (9)初始化 FlashMapManager
	 * Spring MVC Flash Attributes提供了一个请求存储属性，可借其他的请求使用，在使用重定向的时候非常必要，例如 Post/Redirect/Get
	 * 模式，FlashAttributes 在重定向之前暂存，就像存在 session中，以便重定向之后还能使用，并立即删除，
	 * Spring MVC 有两个主要的抽象来支持 flash attributes ,Flash Map  用于保持 flash attributes 而 flashMapManager  用于存储
	 * 检索，管理 flashMap 实例
	 * flashMapManager 在初始化 initFlashMapManager 中完成
	 *
	 */
	private void initFlashMapManager(ApplicationContext context) {
		try {
			this.flashMapManager =
					context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using FlashMapManager [" + this.flashMapManager + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate FlashMapManager with name '" +
						FLASH_MAP_MANAGER_BEAN_NAME + "': using default [" + this.flashMapManager + "]");
			}
		}
	}

	/**
	 * Return this servlet's ThemeSource, if any; else return {@code null}.
	 * <p>Default is to return the WebApplicationContext as ThemeSource,
	 * provided that it implements the ThemeSource interface.
	 * @return the ThemeSource, if any
	 * @see #getWebApplicationContext()
	 */
	public final ThemeSource getThemeSource() {
		if (getWebApplicationContext() instanceof ThemeSource) {
			return (ThemeSource) getWebApplicationContext();
		}
		else {
			return null;
		}
	}

	/**
	 * Obtain this servlet's MultipartResolver, if any.
	 * @return the MultipartResolver used by this servlet, or {@code null} if none
	 * (indicating that no multipart support is available)
	 */
	public final MultipartResolver getMultipartResolver() {
		return this.multipartResolver;
	}

	/**
	 * Return the default strategy object for the given strategy interface.
	 * <p>The default implementation delegates to {@link #getDefaultStrategies},
	 * expecting a single object in the list.
	 * @param context the current WebApplicationContext
	 * @param strategyInterface the strategy interface
	 * @return the corresponding strategy object
	 * @see #getDefaultStrategies
	 */
	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
		List<T> strategies = getDefaultStrategies(context, strategyInterface);
		if (strategies.size() != 1) {
			throw new BeanInitializationException(
					"DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
		}
		return strategies.get(0);
	}

	/**
	 * Create a List of default strategy objects for the given strategy interface.
	 * <p>The default implementation uses the "DispatcherServlet.properties" file (in the same
	 * package as the DispatcherServlet class) to determine the class names. It instantiates
	 * the strategy objects through the context's BeanFactory.
	 * @param context the current WebApplicationContext
	 * @param strategyInterface the strategy interface
	 * @return the List of corresponding strategy objects
	 * 同样在初始化的过程中涉及了一个变量detectAllHandlerAdapters,detectAllHandlerAdapters作用和detectAllHandlerMapping 类似
	 * 只不过作用的对象handlerAdapter ，变可以通过如下的配置来强制系统只加载bean Name 为"hanlerAdapter" handlerAdapter
	 * <init-param>
	 *     <param-name>detectAllHandlerAdapters</param-name>
	 *     <param-value>false</param-value>
	 * </init-param>
	 * 如果无法找到对应的bean ，那么系统会尝试加载默认的配置器
	 * 在getDefaultStrategies 函数中，Spring 会尝试众defaultStrategies 中加载对应的HandlerAdapter属性，那么defalutStrategies
	 * 是如何实现初始化的呢？
	 * ...
	 * ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
	 * 			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
	 * 在系统加载的时候，defaultStrategies 根据当前的路径DispatcherServlet.properties来初始化本身，查看disPatcherServlet.properties
	 * 中对应HandlerAdapter属性
	 *
	 * org.springframework.web.servlet.HandlerAdapter=org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter,\
	 * 	org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter,\
	 * 	org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter
	 *
	 * 	由此得知，如果在程序开发人员没有在配置文件中定义自己的适配器，那么Spring 会默认的加载配置文件中的3个适配器
	 * 	作为总控制器的派遣器servlet通过处理器映射得到处理器后，会轮询处理器适配器模块，查找能够处理当前HTTP 请求的处理器适配器的实现
	 * 	，处理适配器模块根据处理器映射返回的处理器类型，例如简单的控制器类型，注解控制器类型或者远程调用处理器类型，来选择某一个适当的
	 * 处理器适配器实现，从面适配当前的HTTP 请求
	 * HTTP 请求处理器适配器（HttpRequestHandleAdapter）
	 * HTTP 请求处理器适配器仅仅支持矣HTTP 请求处理器的适配，它简单地将HTTP请求对象和响应对象传递给HTTP 请求处理器的实现，它并不需要
	 * 返回值，它主要应用在基于http 的远程调用的实现上
	 * 	简单控制器处理器适配器（SimpleControllerHandlerAdapter ）
	 * 	这个实现类将HTTP请求适配到一个控制咕噜的实现进行处理，这里控制器的实现是一个简单的控制器接口的实现，简单控制吕处理器适配器被设计成一个框架类的
	 * 	的实现，不需要被改写，客户化的业务逻辑通常是在控制器接口的实现类中实现的
	 * 	注解方法处理器适配器（AnnotationMethodHandlerAdapter）
	 * 	这个类的实现是基于注解的实现，它需要结合注解方法映射和注解方法处理器协同工作，它通过解析声明在注解控制器的请求映射信息来解析相应的处理器方法来处理当前的http
	 * 	请求，在处理的过程中，它通过反射来发现探测处理方法的参数，调用处理器方法，并且映射返回值到模型和控制器对象，最后返回模型和控制器对象给作为中主
	 * 	控控制器的派遣器Servlet 。
	 * 		所以我们现在基本上可以回答之前的问题了，Spring 中所使用的Handler 并没有任何特殊的联系，但是为了统一的处理，Spring 提供了不同情况下的适配器
	 *
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
		String key = strategyInterface.getName();
		String value = defaultStrategies.getProperty(key);
		if (value != null) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
			List<T> strategies = new ArrayList<T>(classNames.length);
			for (String className : classNames) {
				try {
					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
					Object strategy = createDefaultStrategy(context, clazz);
					strategies.add((T) strategy);
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherServlet's default strategy class [" + className +
									"] for interface [" + key + "]", ex);
				}
				catch (LinkageError err) {
					throw new BeanInitializationException(
							"Error loading DispatcherServlet's default strategy class [" + className +
									"] for interface [" + key + "]: problem with class file or dependent class", err);
				}
			}
			return strategies;
		}
		else {
			return new LinkedList<T>();
		}
	}

	/**
	 * Create a default strategy.
	 * <p>The default implementation uses {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean}.
	 * @param context the current WebApplicationContext
	 * @param clazz the strategy implementation class to instantiate
	 * @return the fully configured strategy instance
	 * @see ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean
	 */
	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
		return context.getAutowireCapableBeanFactory().createBean(clazz);
	}


	/**
	 * Exposes the DispatcherServlet-specific request attributes and delegates to {@link #doDispatch}
	 * for the actual dispatching.
	 * 我们猜想对请求处理至少应该包括一些诸如寻找Handler 并页面跳转之类的逻辑处理，但是，在doService 中我们并没有看到想看到的
	 * 的逻辑，相反却同样是一些准备工作，但是这些准备工作却是必不可少的，Spring 将已经初始化的功能辅助工具变量比如localeResolver
	 * ,themeResolver等设置request 属性中，而这些属性会在接下来的处理中派上用场
	 * 经过层层的准备工作，终于在doDispatch 函数中看到了完整的请求处理过程
	 *
	 */
	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
			logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
					" processing " + request.getMethod() + " request for [" + getRequestUri(request) + "]");
		}

		// Keep a snapshot of the request attributes in case of an include,
		// to be able to restore the original attributes after the include.
		Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			attributesSnapshot = new HashMap<String, Object>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				if (this.cleanupAfterInclude || attrName.startsWith("org.springframework.web.servlet")) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// Make framework objects available to handlers and view objects.
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
		if (inputFlashMap != null) {
			request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
		}
		request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

		try {
			doDispatch(request, response);
		}
		finally {
			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				// Restore the original attribute snapshot, in case of an include.
				if (attributesSnapshot != null) {
					restoreAttributesAfterInclude(request, attributesSnapshot);
				}
			}
		}
	}

	/**
	 * Process the actual dispatching to the handler.
	 * <p>The handler will be obtained by applying the servlet's HandlerMappings in order.
	 * The HandlerAdapter will be obtained by querying the servlet's installed HandlerAdapters
	 * to find the first that supports the handler class.
	 * <p>All HTTP methods are handled by this method. It's up to HandlerAdapters or handlers
	 * themselves to decide which methods are acceptable.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 * 中央控制器，控制请求的转发
	 */
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				// 检查是否是文件上传请求 ，如果是MultipartContent 类型的request 则转换成 MultipartHttpServletRequest类型的request
				// | MultipartContent 类型的处理request处理
				// 对于请求的处理，Spring 首先考虑的是对Multipart的处理，如果MultipartContent 类型是request ，则转换request
				// 为MultipartHttpServletRequest 类型的request
				processedRequest = checkMultipart(request);

				multipartRequestParsed = (processedRequest != request);
				// Determine handler for the current request.
				// 2.取得处理当前请求的Controller，这里也称为Handler ，即处理器
				// 第一步的意义就是在这里体现了，这里并不是直接返回Controller
				// 该对象封装了Handler和Interceptor | 根据request 寻找Handler
				// | 11.4.2 根据request 信息寻找对应的Handler
				// 在Spring 中闻简单的映射处理器配置台下：
				/**
				 * <bean id="simpleUrlMapping" class="org.Springframework.web.servlet.handler.SimpleUrlHandlerMapping">
				 * 		<property name="mapping">
				 * 		 	<props>
				 * 		 	  	<prop key="/userlist.htm">userController</prop>
				 * 		 	</props>
				 * 		 </property>
				 * </bean>
				 *
				 * 在Spring 加载的过程中，Spring 会将类型为SimpleUrlHandlerMapping 的实例加载到this.handlerMapping 中，按照常理推断
				 * ,根据request 提取对应的Handler ，无非就是提取当前实例中的userController ，但是userController 为了继承自AbstractController
				 * 类型实例，与HandlerExecutionHandler 并无任何关联，那么这一步是如何封装的呢？
				 *
				 */
				mappedHandler = getHandler(processedRequest);
				// 如果Handler 为空，则返回404
				if (mappedHandler == null || mappedHandler.getHandler() == null) {
					// 如果没有找到对应的handler ，则通过response 反馈错误信息
					// | 11.4.3 没有找到对应的Handler 的错误处理
					// 每个请求都应该对应一个Handler，因为每个请求都会在后台有相应的逻辑对应，而逻辑的实现就是在Handler中，所以一旦
					// 遇到没有找到的Handler的情况，正常的情况下如果没有URL匹配的Handler，开发人员可以设置默认的Handler来处理请求
					// 但是默认的请求也未设置的时候就出现Handler为空的情况了，就只能通过response向用户返回错误信息
					noHandlerFound(processedRequest, response);
					return;
				}

				// Determine handler adapter for the current request.
				// 3.获取处理请求的处理器适配器HandlerAdapter | 根据当前的request 寻找对应的HandlerAdapter
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// Process last-modified header, if supported by the handler.
				// 处理last-modified请求头 | 如果当前handler 支持last-modified 头处理
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (logger.isDebugEnabled()) {
						logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
					}
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// Actually invoke the handler.
				// 4.实际处理器处理请求，返回结果视图对象 | 真正的激活Handler 并返回视图
				// | 在研究Spring 对缓存的处理的功能支持前，我们先了解一下一个概念：Last-Modified缓存机制 ，
				// (1)在客户端第一次输入URL的时候服务器端返回内容和状态码200,表示请求成功，同时会添加一个 Last-Modified的响应头，表示此文件在
				// 服务器上的最后更新时间，例如 Last-Modified:Web 14 Mar 2012 10:22:42 GMT 表示最后的更新时间为 2012-03-14 10:20
				// (2) 客户端第二次请求的URL 时，客户端会向服务器发送请求头，if-Modified-Since 询问服务器该时间之后当前请求的内容是不中有被
				// 修改过，如 if-MOdified-Since: Web 。14 Mar 2012 10：22：42 GMT ，如果
				// If-Modified-Since ：Web 14 Mar 2012 10:22:42 GMT 如果服务器端的内容没有就业率，则自动返回HTTP 304 状态码，
				// 只要响应头为空，这样就节省了网络带宽
				// Spring 提供了Last-Modified机制的支持，只需要实现LastModified 接口，如下所示
				// public class HelloWorldLastModifiedCacheController extends AbstractController implements LastModified {
				// 		private long lastModified ;
				// 		protected ModelAndView handleRequestInternal(HttpServletRequest req ,HttpServletResponse resp) throw exception {
				//			//点击后再次请求当前页面
				//			resp.getWriter().write("<a href=''>this</a>")
				//			return null;
				//		}
				//		public long getLastModified(HttpServletRequest request ){
				//			if(lastModified == 0L){
				//				// 第一次或者逻辑有变化的时候，应该重新返回内容最新修改时间戳
				//				lastModified = System.currentTimeMillis();
				//			}
				//		}
				// }
				// HelloWorldLastModifiedCacheController 只需要实现LastModified 接口的getLastModified()方法保证当内容发生改变时返回了
				// 最新的修改时间即可
				// Spring 判断是否过期，通过判断请求 If-Modified-Since 是否大于等于当前的getLastModified 方法的时间戳，如果是
				// 则认为没有修改，上面的controller 与普通的controller 并没有太大的差别，声明如下
				// <bean name="/helloLastModified" class="com.test.controller.HelloWorldLastModifiedCacheController"></bean>
				// 11.4.6 HandlerInterceptor处理器
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}
				// 结果视图对象的处理 | 视图名称转换应用于需要添加前缀后缀的情况
				applyDefaultViewName(processedRequest, mv);
				// 应用所有拦截器postHandle方法
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {

			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Error err) {
			triggerAfterCompletionWithError(processedRequest, response, mappedHandler, err);
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				if (mappedHandler != null) {
					// 请求成功响应之后的方法
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// Clean up any resources used by a multipart request.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}

	/**
	 * Do we need view name translation?
	 */
	private void applyDefaultViewName(HttpServletRequest request, ModelAndView mv) throws Exception {
		if (mv != null && !mv.hasView()) {
			mv.setViewName(getDefaultViewName(request));
		}
	}

	/**
	 * Handle the result of handler selection and handler invocation, which is
	 * either a ModelAndView or an Exception to be resolved to a ModelAndView.
	 * doDispatch 函数中展示了Spring 请求的处理所涉及的主要逻辑，而我们之前设置的request 中的各种辅助属性也都有被派上了用场，
	 * 下面回顾一下逻辑处理的全部过程
	 *
	 */
	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) throws Exception {

		boolean errorView = false;

		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			}
			else {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// Did the handler return a view to render?
		// 如果在Handler 实例的处理中返回了view ,那么需要做页面的处理
		if (mv != null && !mv.wasCleared()) {
			// 处理页面跳转
			render(mv, request, response);
			if (errorView) {
				WebUtils.clearErrorRequestAttributes(request);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Null ModelAndView returned to DispatcherServlet with name '" + getServletName() +
						"': assuming HandlerAdapter completed request handling");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// Concurrent handling started during a forward
			return;
		}

		if (mappedHandler != null) {
			// 完成处理激活触发器
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}

	/**
	 * Build a LocaleContext for the given request, exposing the request's primary locale as current locale.
	 * <p>The default implementation uses the dispatcher's LocaleResolver to obtain the current locale,
	 * which might change during a request.
	 * @param request current HTTP request
	 * @return the corresponding LocaleContext
	 */
	@Override
	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
		if (this.localeResolver instanceof LocaleContextResolver) {
			return ((LocaleContextResolver) this.localeResolver).resolveLocaleContext(request);
		}
		else {
			return new LocaleContext() {
				@Override
				public Locale getLocale() {
					return localeResolver.resolveLocale(request);
				}
			};
		}
	}

	/**
	 * Convert the request into a multipart request, and make multipart resolver available.
	 * <p>If no multipart resolver is set, simply use the existing request.
	 * @param request current HTTP request
	 * @return the processed request (multipart wrapper if necessary)
	 * @see MultipartResolver#resolveMultipart
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
				logger.debug("Request is already a MultipartHttpServletRequest - if not in a forward, " +
						"this typically results from an additional MultipartFilter in web.xml");
			}
			else if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) instanceof MultipartException) {
				logger.debug("Multipart resolution failed for current request before - " +
						"skipping re-resolution for undisturbed error rendering");
			}
			else {
				return this.multipartResolver.resolveMultipart(request);
			}
		}
		// If not returned before: return original request.
		return request;
	}

	/**
	 * Clean up any resources used by the given multipart request (if any).
	 * @param request current HTTP request
	 * @see MultipartResolver#cleanupMultipart
	 */
	protected void cleanupMultipart(HttpServletRequest request) {
		MultipartHttpServletRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
		if (multipartRequest != null) {
			this.multipartResolver.cleanupMultipart(multipartRequest);
		}
	}

	/**
	 * Return the HandlerExecutionChain for this request.
	 * <p>Tries all handler mappings in order.
	 * @param request current HTTP request
	 * @return the HandlerExecutionChain, or {@code null} if no handler could be found
	 * getHandler(ProcessedRequest) 方法实际上从HandlerMapping中找到URL和Controller对应关系，也就是Map<url,Controller>
	 * ,我们知道，最终处理请求的是Controller中的方法，现在只是知道了Controller，如何确认Controller中处理请求的方法呢？
	 * 最后调用RequestMappingHandlerAdapter的Handler()中的核心代码来，由于HandleInternal(request,response，Handler)实现
	 * 整个处理过程中最核心的步骤就是拼接Controller的URL 和方法的URL,与request的URL 进行匹配，找到匹配的方法
	 * 根据URL 获取处理请求的方法
	 *
	 *
	 *
	 *
	 *
	 * |
	 *
	 *
	 * 在之前的内容我们提过，在系统启动时，Spring 会将所有的映射类型的bean 注册到this.handlerMapping的变量中，所以此函数的目的就是
	 * 遍历所有的HandlerMapping ，并调用其getHandler 方法进行封装，以SimpleUrlHandlerMapping 为例，查看getHandler 方法如下
	 */
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		for (HandlerMapping hm : this.handlerMappings) {
			if (logger.isTraceEnabled()) {
				logger.trace(
						"Testing handler map [" + hm + "] in DispatcherServlet with name '" + getServletName() + "'");
			}
			HandlerExecutionChain handler = hm.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

	/**
	 * No handler found -> set appropriate HTTP response status.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if preparing the response failed
	 */
	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			pageNotFoundLogger.warn("No mapping found for HTTP request with URI [" + getRequestUri(request) +
					"] in DispatcherServlet with name '" + getServletName() + "'");
		}
		if (this.throwExceptionIfNoHandlerFound) {
			ServletServerHttpRequest sshr = new ServletServerHttpRequest(request);
			throw new NoHandlerFoundException(
					sshr.getMethod().name(), sshr.getServletRequest().getRequestURI(), sshr.getHeaders());
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * Return the HandlerAdapter for this handler object.
	 * @param handler the handler object to find an adapter for
	 * @throws ServletException if no HandlerAdapter can be found for the handler. This is a fatal error.
	 * 11.4.4 根据当前Handler寻找对应的HandlerAdapter
	 * 在WebApplicationContext 的初始化过程中我们讨论了HandlerAdapters 的初始化的，了解了在默认的情况下普通Web请求会交给
	 * SimpleControllerHandlerAdapter去处理，下面我们以SimpleControllerHandlerAdaptor为例来分析获取适配器的逻辑
	 *
	 *
	 * |
	 *
	 * 通过这个函数我们了解到，对于获取适配器的逻辑无非就是遍历所有的适配器来选择合适的适配器并返回它，而某个适配器是否适用于当前的Handler
	 * 逻辑被封装在具体的适配器中，进一步查看SimpleControllerHandlerAdaptor中的supports
	 * public boolean supports(Object handler){
	 *     return (Handler instanceof Controller);
	 * }
	 * 分析到这里，一切都已经明了了，SimpleControllerHandlerAdapter 就是用于处理普通的Web请求，而对应于SpringMVC 来说，我们会把
	 * 逻辑封装至Controller的子类中，例如我们之前引导的示例，UserController 就是继承自AbstractController ，而AbstractController
	 * 的实现Controller 接口
	 *
	 */
	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		for (HandlerAdapter ha : this.handlerAdapters) {
			if (logger.isTraceEnabled()) {
				logger.trace("Testing handler adapter [" + ha + "]");
			}
			if (ha.supports(handler)) {
				return ha;
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

	/**
	 * Determine an error ModelAndView via the registered HandlerExceptionResolvers.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
	 * (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to
	 * @throws Exception if no error ModelAndView found
	 * 有时候系统运行过程中出现异常，而我们并不希望就此中断对用户的服务，而是至少告知客户当前系统在处理逻辑的过程上出现的异常，甚至告知他们因为什么
	 * 原因导致的，Spring 中的异常处理机制会帮我们完成这个工作的，其实，这里Spring 主要的工作就是将逻辑引导到HandlerExceptionResolver
	 * 类的resolverException方法，而HandlerExceptionResolver的使用，我们在讲解WebApplicationContext 的初始化的时候已经介绍过
	 *
	 */
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		// Check registered HandlerExceptionResolvers...
		ModelAndView exMv = null;
		for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
			//
			exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
			if (exMv != null) {
				break;
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// We might still need view name translation for a plain error model...
			if (!exMv.hasView()) {
				exMv.setViewName(getDefaultViewName(request));
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Handler execution resulted in exception - forwarding to resolved error view: " + exMv, ex);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		throw ex;
	}

	/**
	 * Render the given ModelAndView.
	 * <p>This is the last stage in handling a request. It may involve resolving the view by name.
	 * @param mv the ModelAndView to render
	 * @param request current HTTP servlet request
	 * @param response current HTTP servlet response
	 * @throws ServletException if view is missing or cannot be resolved
	 * @throws Exception if there's a problem rendering the view
	 * 11.4.9 根据视图的跳转页面
	 * 无论是一个系统还是一个站点，最重要的工作就是与用户进行交互，用户操作系统后无论下发命令是不是成功与否，都需要给用户一个反馈
	 * ，以便于肪进行下一步的判断，所以，在逻辑处理的最后一定涉及一个页面跳转问题
	 *
	 */
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Determine locale for request and apply it to the response.
		Locale locale = this.localeResolver.resolveLocale(request);
		response.setLocale(locale);

		View view;
		if (mv.isReference()) {
			// We need to resolve the view name.
			view = resolveViewName(mv.getViewName(), mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		}
		else {
			// No need to lookup: the ModelAndView object contains the actual View object.
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// Delegate to the View object for rendering.
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering view [" + view + "] in DispatcherServlet with name '" + getServletName() + "'");
		}
		try {
			view.render(mv.getModelInternal(), request, response);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "] in DispatcherServlet with name '" +
						getServletName() + "'", ex);
			}
			throw ex;
		}
	}

	/**
	 * Translate the supplied request into a default view name.
	 * @param request current HTTP servlet request
	 * @return the view name (or {@code null} if no default found)
	 * @throws Exception if view name translation failed
	 */
	protected String getDefaultViewName(HttpServletRequest request) throws Exception {
		return this.viewNameTranslator.getViewName(request);
	}

	/**
	 * Resolve the given view name into a View object (to be rendered).
	 * <p>The default implementations asks all ViewResolvers of this dispatcher.
	 * Can be overridden for custom resolution strategies, potentially based on
	 * specific model attributes or request parameters.
	 * @param viewName the name of the view to resolve
	 * @param model the model to be passed to the view
	 * @param locale the current locale
	 * @param request current HTTP servlet request
	 * @return the View object, or {@code null} if none found
	 * @throws Exception if the view cannot be resolved
	 * (typically in case of problems creating an actual View object)
	 * @see ViewResolver#resolveViewName
	 * 解析视图名称
	 * 在上下文中我们提到了DispatcherServlet 会根据ModelAndView选择合适的视图来进行渲染，而这一功能就是resolverViewName 函数中完成
	 *
	 */
	protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale,
			HttpServletRequest request) throws Exception {

		for (ViewResolver viewResolver : this.viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				return view;
			}
		}
		return null;
	}

	private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	private void triggerAfterCompletionWithError(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, Error error) throws Exception {

		ServletException ex = new NestedServletException("Handler processing failed", error);
		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	/**
	 * Restore the request attributes after an include.
	 * @param request current HTTP request
	 * @param attributesSnapshot the snapshot of the request attributes before the include
	 */
	@SuppressWarnings("unchecked")
	private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?,?> attributesSnapshot) {
		// Need to copy into separate Collection here, to avoid side effects
		// on the Enumeration when removing attributes.
		Set<String> attrsToCheck = new HashSet<String>();
		Enumeration<?> attrNames = request.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = (String) attrNames.nextElement();
			if (this.cleanupAfterInclude || attrName.startsWith("org.springframework.web.servlet")) {
				attrsToCheck.add(attrName);
			}
		}

		// Add attributes that may have been removed
		attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());

		// Iterate over the attributes to check, restoring the original value
		// or removing the attribute, respectively, if appropriate.
		for (String attrName : attrsToCheck) {
			Object attrValue = attributesSnapshot.get(attrName);
			if (attrValue == null){
				request.removeAttribute(attrName);
			}
			else if (attrValue != request.getAttribute(attrName)) {
				request.setAttribute(attrName, attrValue);
			}
		}
	}

	private static String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return uri;
	}

}
