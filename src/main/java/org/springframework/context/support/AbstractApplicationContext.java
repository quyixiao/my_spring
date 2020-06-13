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

package org.springframework.context.support;

import com.test.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.*;
import org.springframework.context.event.*;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of the {@link ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors}
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as "applicationEventMulticaster" bean
 * of type {@link ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading through extending
 * {@link DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overwritten in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 * @since January 21, 2001
 */

@Slf4j
public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext, DisposableBean {

    /**
     * Name of the MessageSource bean in the factory.
     * If none is supplied, message resolution is delegated to the parent.
     *
     * @see MessageSource
     */
    public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

    /**
     * Name of the LifecycleProcessor bean in the factory.
     * If none is supplied, a DefaultLifecycleProcessor is used.
     *
     * @see org.springframework.context.LifecycleProcessor
     * @see DefaultLifecycleProcessor
     */
    public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

    /**
     * Name of the ApplicationEventMulticaster bean in the factory.
     * If none is supplied, a default SimpleApplicationEventMulticaster is used.
     *
     * @see ApplicationEventMulticaster
     * @see SimpleApplicationEventMulticaster
     */
    public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

    // 静态初始化块，在整个容器创建的过程中只执行一次
    static {
        // Eagerly load the ContextClosedEvent class to avoid weird classloader issues
        // on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
        // 为了避免应用程序在Weblogic 8.1 关闭旪出现的类加载异常问题，加载Ioc 容器关闭事件类
        ContextClosedEvent.class.getName();
    }


    /**
     * Logger used by this class. Available to subclasses.
     */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Unique id for this context, if any
     */
    private String id = ObjectUtils.identityToString(this);

    /**
     * Display name
     */
    private String displayName = ObjectUtils.identityToString(this);

    /**
     * Parent context
     */
    private ApplicationContext parent;

    /**
     * Environment used by this context
     */
    private ConfigurableEnvironment environment;

    /**
     * BeanFactoryPostProcessors to apply on refresh
     */
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors =
            new ArrayList<BeanFactoryPostProcessor>();

    /**
     * System time in milliseconds when this context started
     */
    private long startupDate;

    /**
     * Flag that indicates whether this context is currently active
     */
    private final AtomicBoolean active = new AtomicBoolean();

    /**
     * Flag that indicates whether this context has been closed already
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Synchronization monitor for the "refresh" and "destroy"
     */
    private final Object startupShutdownMonitor = new Object();

    /**
     * Reference to the JVM shutdown hook, if registered
     */
    private Thread shutdownHook;

    /**
     * ResourcePatternResolver used by this context
     */
    private ResourcePatternResolver resourcePatternResolver;

    /**
     * LifecycleProcessor for managing the lifecycle of beans within this context
     */
    private LifecycleProcessor lifecycleProcessor;

    /**
     * MessageSource we delegate our implementation of this interface to
     */
    private MessageSource messageSource;

    /**
     * Helper class used in event publishing
     */
    private ApplicationEventMulticaster applicationEventMulticaster;

    /**
     * Statically specified listeners
     */
    private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<ApplicationListener<?>>();

    /**
     * ApplicationEvents published early
     */
    private Set<ApplicationEvent> earlyApplicationEvents;


    /**
     * Create a new AbstractApplicationContext with no parent.
     */
    public AbstractApplicationContext() {
        this.resourcePatternResolver = getResourcePatternResolver();
    }

    /**
     * Create a new AbstractApplicationContext with the given parent context.
     *
     * @param parent the parent context
     */
    public AbstractApplicationContext(ApplicationContext parent) {
        this();
        setParent(parent);
    }


    //---------------------------------------------------------------------
    // Implementation of ApplicationContext interface
    //---------------------------------------------------------------------

    /**
     * Set the unique id of this application context.
     * <p>Default is the object id of the context instance, or the name
     * of the context bean if the context is itself defined as a bean.
     *
     * @param id the unique id of the context
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getApplicationName() {
        return "";
    }

    /**
     * Set a friendly name for this context.
     * Typically done during initialization of concrete context implementations.
     * <p>Default is the object id of the context instance.
     */
    public void setDisplayName(String displayName) {
        Assert.hasLength(displayName, "Display name must not be empty");
        this.displayName = displayName;
    }

    /**
     * Return a friendly name for this context.
     *
     * @return a display name for this context (never {@code null})
     */
    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Return the parent context, or {@code null} if there is no parent
     * (that is, this context is the root of the context hierarchy).
     */
    @Override
    public ApplicationContext getParent() {
        return this.parent;
    }

    /**
     * {@inheritDoc}
     * <p>If {@code null}, a new environment will be initialized via
     * {@link #createEnvironment()}.
     */
    @Override
    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    /**
     * {@inheritDoc}
     * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
     * default with this method is one option but configuration through {@link
     * #getEnvironment()} should also be considered. In either case, such modifications
     * should be performed <em>before</em> {@link #refresh()}.
     *
     * @see AbstractApplicationContext#createEnvironment
     */
    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Return this context's internal bean factory as AutowireCapableBeanFactory,
     * if already available.
     *
     * @see #getBeanFactory()
     */
    @Override
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return getBeanFactory();
    }

    /**
     * Return the timestamp (ms) when this context was first loaded.
     */
    @Override
    public long getStartupDate() {
        return this.startupDate;
    }

    /**
     * Publish the given event to all listeners.
     * <p>Note: Listeners get initialized after the MessageSource, to be able
     * to access it within listener implementations. Thus, MessageSource
     * implementations cannot publish events.
     *
     * @param event the event to publish (may be application-specific or a
     *              standard framework event)
     */
    @Override
    public void publishEvent(ApplicationEvent event) {
        publishEvent(event, null);
    }

    /**
     * Publish the given event to all listeners.
     * <p>Note: Listeners get initialized after the MessageSource, to be able
     * to access it within listener implementations. Thus, MessageSource
     * implementations cannot publish events.
     *
     * @param event the event to publish (may be an {@link ApplicationEvent}
     *              or a payload object to be turned into a {@link PayloadApplicationEvent})
     */
    @Override
    public void publishEvent(Object event) {
        publishEvent(event, null);
    }

    /**
     * Publish the given event to all listeners.
     *
     * @param event     the event to publish (may be an {@link ApplicationEvent}
     *                  or a payload object to be turned into a {@link PayloadApplicationEvent})
     * @param eventType the resolved event type, if known
     * @since 4.2
     * 当完成 ApplicationContext 初始化的时候，要通过 Spring 中的事件发布机制来发出 ContextRefreshedEvent 事件，以保证对应的
     * 监听器可以做进一步的逻辑处理
     *
     */
    protected void publishEvent(Object event, ResolvableType eventType) {
        Assert.notNull(event, "Event must not be null");
        if (logger.isTraceEnabled()) {
            logger.trace("Publishing event in " + getDisplayName() + ": " + event);
        }

        // Decorate event as an ApplicationEvent if necessary
        ApplicationEvent applicationEvent;
        if (event instanceof ApplicationEvent) {
            applicationEvent = (ApplicationEvent) event;
        } else {
            applicationEvent = new PayloadApplicationEvent<Object>(this, event);
            if (eventType == null) {
                eventType = ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, event.getClass());
            }
        }

        // Multicast right now if possible - or lazily once the multicaster is initialized
        if (this.earlyApplicationEvents != null) {
            this.earlyApplicationEvents.add(applicationEvent);
        } else {
            getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
        }

        // Publish event via parent context as well...
        if (this.parent != null) {
            if (this.parent instanceof AbstractApplicationContext) {
                ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
            } else {
                this.parent.publishEvent(event);
            }
        }
    }

    /**
     * Return the internal ApplicationEventMulticaster used by the context.
     *
     * @return the internal ApplicationEventMulticaster (never {@code null})
     * @throws IllegalStateException if the context has not been initialized yet
     */
    ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
        if (this.applicationEventMulticaster == null) {
            throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
                    "call 'refresh' before multicasting events via the context: " + this);
        }
        return this.applicationEventMulticaster;
    }

    /**
     * Return the internal LifecycleProcessor used by the context.
     *
     * @return the internal LifecycleProcessor (never {@code null})
     * @throws IllegalStateException if the context has not been initialized yet
     */
    LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
        if (this.lifecycleProcessor == null) {
            throw new IllegalStateException("LifecycleProcessor not initialized - " +
                    "call 'refresh' before invoking lifecycle methods via the context: " + this);
        }
        return this.lifecycleProcessor;
    }

    /**
     * Return the ResourcePatternResolver to use for resolving location patterns
     * into Resource instances. Default is a
     * {@link PathMatchingResourcePatternResolver},
     * supporting Ant-style location patterns.
     * <p>Can be overridden in subclasses, for extended resolution strategies,
     * for example in a web environment.
     * <p><b>Do not call this when needing to resolve a location pattern.</b>
     * Call the context's {@code getResources} method instead, which
     * will delegate to the ResourcePatternResolver.
     *
     * @return the ResourcePatternResolver for this context
     * @see #getResources
     * @see PathMatchingResourcePatternResolver
     */
    // 获取一个Spring Source 加载器用于读入Spring Bean 的配置信息
    protected ResourcePatternResolver getResourcePatternResolver() {
        // AbstractApplicationContext 继承DefaultResourceLoader，因此是一个资源加载器
        // Spring 没资源加载器getResource(String location) 方法用于载入资源
        return new PathMatchingResourcePatternResolver(this);
    }


    //---------------------------------------------------------------------
    // Implementation of ConfigurableApplicationContext interface
    //---------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
     * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
     * this (child) application context environment if the parent is non-{@code null} and
     * its environment is an instance of {@link ConfigurableEnvironment}.
     *
     * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
     */
    @Override
    public void setParent(ApplicationContext parent) {
        this.parent = parent;
        if (parent != null) {
            Environment parentEnvironment = parent.getEnvironment();
            if (parentEnvironment instanceof ConfigurableEnvironment) {
                getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
            }
        }
    }

    @Override
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
        this.beanFactoryPostProcessors.add(beanFactoryPostProcessor);
    }


    /**
     * Return the list of BeanFactoryPostProcessors that will get applied
     * to the internal BeanFactory.
     */
    public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
        return this.beanFactoryPostProcessors;
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        if (this.applicationEventMulticaster != null) {
            this.applicationEventMulticaster.addApplicationListener(listener);
        } else {
            this.applicationListeners.add(listener);
        }
    }

    /**
     * Return the list of statically specified ApplicationListeners.
     */
    public Collection<ApplicationListener<?>> getApplicationListeners() {
        return this.applicationListeners;
    }

    /**
     * Create and return a new {@link StandardEnvironment}.
     * <p>Subclasses may override this method in order to supply
     * a custom {@link ConfigurableEnvironment} implementation.
     */
    protected ConfigurableEnvironment createEnvironment() {
        return new StandardEnvironment();
    }

    /***
     * refresh()方法主要为IoC容器Bean的生命周期管理提供条件，Spring Ioc 容器载入了Bean的配置，信息从其子类容器的refreshBeanFactory
     * 方法启动，所以整个refresh()方法中的"ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory" 以后的代码都是在
     * 注册容器信息源和生命周期事件，我们前面说的载入就是通过这个代码启动的
     *
     * refresh()方法的主要作是： 在创建Ioc容器前，如果已经有容器存在，需要把已有的容器销毁和关闭，以保证在refresh()方法之后的使用的是新
     * 创创建的Ioc容器，它类似于对Ioc容器的重启，在新创建的容器中对容器进行初始化，对Bean配置资源进行载入
     *
     *
     * 我们已经知道，IoC容器初始化过程就是对Bean定义资源的定位，载入和注册，此时容器对Bean的依赖注入并没有发生，依赖注入是在应用程序
     * 第一次向索取Bean时通过getBean()方法来完成的
     *
     * 当Bean定义资源的Bean元素中配置了lazy-init=false 属性时，容器将会在初始化时对所配置的Bean进行实例化，Bean的依赖注入在容器在容器
     * 初始化时就已经完成，这样应用程序第一次向容器索取被管理的Bean时，就不用再初始化和对应的Bean进行依赖注入了，而是直接从容器中获取
     * 已经完成依赖注入Bean,提高了应用程序第一次向容器获取Bean的性能
     *
     * refresh()方法
     * IOC 容器读入已经定位的Bean 定义资源是从refresh()方法开始的，我们从AbstractApplicationContext类的refresh()方法入手分析
     *
     * 1.下面概括一下 ClassPathXmlApplicationContext 初始化的步骤，并从中解释一下它为我们提供的功能
     * 1. 初始化前的准备工作，例如对系统属性或者环境变量进行准备及验证
     * 在某种情况下，项目的使用需要读取某些系统变量，而这个变量的设置很可能会影响到系统的正确性，那么 ClassPathXmlApplicationContext
     * 为我们提供的准备函数就显得非常的必要了，它可以在 Spring  启动的时候提前对必需的变量进行存在性验证
     * 2.初始化 beanFactory 进行 Xml 文件的读取
     * 之前有提到的 ClasspathXmlApplicationContext包含着 BeanFactory 所提供的一切特征，在这一步骤中将会复用 BeanFActory 中的配置
     * 文件读取及解析其他的功能，这一步之后，ClassPathXmlApplicationContext 实际上就已经包含了 BeanFactory 所提供的功能，也就是可以
     * 进行 Bean 的提取等基础操作了
     * 3.对 BeanFactory 进行各种功能的填充
     * @Qualifier 与@Autowired 应该是大家非常熟悉的注解了，那么这两个注册正是这一步骤增加的支持
     * 4.Spring 之所以强大，除了它功能上为大家提供了便例外，还有一方面它的完美架构，开放式的架构让使用它的程序员很容易根据业务需要扩展
     * 已经存在的功能，这种开放式的设置在 Spring中随处可见，例如在配合中就提供了一个空间函数的实现，postProcessBeanFactory 来方便程序员
     * 在业务上做进步的扩展
     * 5. 激活各种 beanFactory 的处理器
     * 6.注册拦截 bean 创建的 bean 处理器，这里只是注册，真正的调用是在 getBean时候
     * 7.为上下文初始化 Message源，即对不同的语言的消息进行国际化的处理
     * 8. 初始化应用的消息广播器，并放入到"applicationEventMulticaster" bean 中
     * 9.留给子类来初始化其他的 bean
     * 10,所有的注册的 bean 中查找 listener bean 注册到的消息广播器中
     * 11.初始化剩下的单例（非惰性的）
     * 12. 完成刷新的过程，通知生命周期处理器 lifecycleProcessor 刷新过程，同时发出 contextRefreshEvent 通知别人
     *
     *
     */
    @Override
    public void refresh() throws BeansException, IllegalStateException {
        synchronized (this.startupShutdownMonitor) {
            // Prepare this context for refreshing.
            log.info("start prepareRefresh");
            // 1.调用容器准备刷新的方法，获取容器的当前时间，同时给容器设置同步标识
            prepareRefresh();
            log.info("end prepareRefresh");

            // Tell the subclass to refresh the internal bean factory.
            // 告诉子类启动refreshBeanFactory()方法，Bean定义资源文件的载入从子类 的refreshBeanFactory()方法启动
            // 在refresh()方法中 ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory() 启动了Bean的注册
            // Bean定义资源的载入，注册过程，finishBeanFactoryInitialization() 方法是对注册后的Bean定义中的预实例化(lazy-init=false)
            // Spring 默认进行预实例化，即为true的Bean 进行处理的地方
            // 初始化 bean ，并野德 xml 文件的读取
            // obtainFreshBeanFactory 方法从字面的理解是获取 BeanFactory ，之前有说过，ApplicationContext 是对 BeanFactory
            // 的功能上基础上添加了大量的扩展应用，那么 obtainFreshBeanFactory 正是实现 BeanFactory 的地方，也就是经过这个函数之后
            // ApplicationContext 就已经拥有 BeanFactory 的全部功能
            log.info("start obtainFreshBeanFactory");
            // | 这里是在子类中启动refreshBeanFactory()的地方
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
            log.info("end obtainFreshBeanFactory");


            log.info("start prepareBeanFactory");
            // Prepare the bean factory for use in this context.
            // 3.为BeanFactory配置容器，例如类加载器，事件处理器 | 为 BeanFactory 进行各种功能进行填充
            prepareBeanFactory(beanFactory);
            log.info("end prepareBeanFactory");

            try {
                // Allows post-processing of the bean factory in context subclasses.
                // 4.为容器的某些子类指定特殊的Post事件处理器 |  子类覆盖方法做额外的处理 | 
                log.info("start postProcessBeanFactory");
                postProcessBeanFactory(beanFactory);
                log.info("end prepareBeanFactory");

                // Invoke factory processors registered as beans in the context.
                // 5.调用所有的注册的beanFactoryPostProcessor的bean | 激活各种 BeanFactory 处理器
                log.info("start invokeBeanFactoryPostProcessors");
                invokeBeanFactoryPostProcessors(beanFactory);
                log.info("end invokeBeanFactoryPostProcessors");

                // Register bean processors that intercept bean creation.
                // 6.为BeanFactory注册Post事件处理器
                // BeanPostProcessor(BeanFactory)
                log.info("start registerBeanPostProcessors");
                // 注册拦截 bean 创建 bean 处理器，这里只是注册，真正的调用在 getBean 时候
                registerBeanPostProcessors(beanFactory);
                log.info("end registerBeanPostProcessors");

                // Initialize message source for this context.
                // 初始化信息源，和国际化相关 |  为上下文初始化 Message源，即不同的语言的消息体，国际化处理
                log.info("start initMessageSource");
                initMessageSource();
                log.info("end initMessageSource");

                // Initialize event multicaster for this context.
                // 8.初始化容器事件传播器 | 初始化应用消息广播器，并放入到"applicationEventMulticaster" bean 中
                log.info("start initApplicationEventMulticaster");



                initApplicationEventMulticaster();
                log.info("end initApplicationEventMulticaster");

                // Initialize other special beans in specific context subclasses.
                // 9.调用子类的某些特殊bean的初始化方法 | 留给子类来初始化其他的 Bean
                log.info("start onRefresh");
                onRefresh();
                log.info("end onRefresh");

                // Check for listener beans and register them.
                // 为事件传播器注册事件监听器 | 在所有的注册的 bean 中查找 Listener Bean ,注册到消息广播器中
                log.info("start registerListeners");
                registerListeners();
                log.info("end registerListeners");

                // Instantiate all remaining (non-lazy-init) singletons.
                // 11.初始化所有剩余的单例Bean
                log.info("start finishBeanFactoryInitialization");
                finishBeanFactoryInitialization(beanFactory);
                log.info("end finishBeanFactoryInitialization");

                // Last step: publish corresponding event.
                // 12.初始化容器的生命周期事件处理器，为发布容器的生命周期事件 |  完成刷新过程，通知生命周期处理器 lifecycleProcessor 刷新
                // 过程，同时发出 contextRefreshEvent 通知别人
                log.info("start finishRefresh");
                finishRefresh();
                LogUtils.info("end finishRefresh" ,3);
            } catch (BeansException ex) {
                logger.warn("Exception encountered during context initialization - cancelling refresh attempt", ex);

                // Destroy already created singletons to avoid dangling resources.
                // 13.销毁已经创建的bean
                log.info("start destroyBeans");
                destroyBeans();
                log.info("end destroyBeans");

                // Reset 'active' flag.
                // 14.取消刷新操作，重置容器的同步标识
                log.info("start cancelRefresh");
                cancelRefresh(ex);
                log.info("end cancelRefresh");

                // Propagate exception to caller.
                throw ex;
            } finally {
                // Reset common introspection caches in Spring's core, since we
                // might not ever need metadata for singleton beans anymore...
                log.info("start resetCommonCaches");
                // 设置公共缓存
                resetCommonCaches();
                log.info("end resetCommonCaches");
            }
        }
    }

    /**
     * Prepare this context for refreshing, setting its startup date and
     * active flag as well as performing any initialization of property sources.
     * 网上有人说其实这个函数没有什么用，因为最后两句代码才是最关键，但是却没有逻辑处理，initPropertySources 是空的，没有任何逻辑
     * ，而 getEnvironment().validateRequiredProperties 也因为没有需要验证的属性而没有做任何处理，其实这都是因为没有彻底的理解才会
     * 这样说，这个函数如果用好了，作用不还是很大的，那么，该怎样的用呢？ 我们先来探索下各个函数的作用
     *
     * 1.initPropertySources 正符合 Spring 开放式结构设计，给用户最大的扩展 Spring 的能力，用户可以根据自身的需要重写 initPropertySources方法
     * ，并在这个方法中进行修改化的属性处理及设置
     * 2.validateRequiredProperties 则是对属性进行验证，那么如何验证呢？我们举个融合两个代码的小例子来帮助大家理解
     * 假如：现在有这样的一个需求，工程在运行的过中用到了某个设置 例如 VAR 是从系统环境变量中取得的，而如果用户没有在系统环境中配置这个参数
     * 那么工程可能不会工作，这一要求可能会有各种各样的解决办法，当然，在 Spring 中可以这样做，你可以直接修改 Spring 的源码，例如修改
     * ClasspathXmlApplicationContext ，当然，最好的办法还是对源码进行扩展，我们可以自定义类
     * public class MyClassPathApplicationContext extends ClassPathXmlApplicationContext {
     *     public MyClassPathApplicationContext (String ... configLocations){
     *         super(configLocations);
     *     }
     *     protected void initPropertySources(){
     *         // 添加验证要求
     *         getEnviroment().setRequiredProperties("VAR");
     *     }
     * }
     * 我们自定义了继承自 ClassPathXmlApplicationContext 的 MyClassPathXmlApplicationContext，并重写了 initPropertySources方法
     * 在方法中，我们添加了我们的修改化需求，那么验证的时候也就是程序直到了 getEnvironment().validateRequiredProperties()代码的时候
     * 如果系统并没有检测到对应的 VAR 的环境变量，那么就抛出异常，当然，我们还需要在使用的时候替换掉原有的 ClassPathXmlApplicationContext
     *
     */
    protected void prepareRefresh() {
        this.startupDate = System.currentTimeMillis();
        this.active.set(true);

        if (logger.isInfoEnabled()) {
            logger.info("Refreshing " + this);
        }

        // Initialize any placeholder(占位符) property sources in the context environment
        log.info(" start initPropertySources");
        // 留给子类覆盖
        initPropertySources();
        log.info(" end initPropertySources");

        // Validate that all properties marked as required are resolvable
        // see ConfigurablePropertyResolver#setRequiredProperties
        /**
         *    {@link ConfigurableEnvironment },
         */
        // 验证需要的属性文件中是否都已经放入到环境中
        getEnvironment().validateRequiredProperties();

        // Allow for the collection of early ApplicationEvents,
        // to be published once the multicaster is available...
        this.earlyApplicationEvents = new LinkedHashSet<ApplicationEvent>();
    }

    /**
     * <p>Replace any stub property sources with actual instances.
     *
     * @see org.springframework.core.env.PropertySource.StubPropertySource
     * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
     */
    protected void initPropertySources() {
        log.info("initPropertySources ing ");
        // For subclasses: do nothing by default.
    }

    /**
     * Tell the subclass to refresh the internal bean factory.
     *
     * @return the fresh BeanFactory instance
     * @see #refreshBeanFactory()
     * @see #getBeanFactory()
     */
    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        log.info(" pre refreshBeanFactory ");
        // 这里使用了委派模式，父类定义了抽象的refreshBeanFactory()方法
        // 具体实现调用了子类容器的refreshBeanFatory()方法民| 初始化 BeanFactory ，并进行 xml 文件的读取，并将得到的 BeanFactory
        // 记录在当前实体的属性中
        refreshBeanFactory();
        // 返回当前实例的 BeanFactory 属性
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (logger.isDebugEnabled()) {
            logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
        }
        return beanFactory;
    }

    /**
     * Configure the factory's standard context characteristics,
     * such as the context's ClassLoader and post-processors.
     *
     * @param beanFactory the BeanFactory to configure
     *
     * @return
     * 进入函数 prepareBeanFactory 前，Spring 已经完成了配置的解析，而 ApplicationContext 的功能的扩展也由此展开
     * 上面函数中主要进行了几个方面的扩展
     * 1.增加对 SPEL 语言的支持
     * 2.增加对属性编辑器的支持
     * 3.增加对一些内置类，比如 EnvironmentAware,MessageSourceAware 的信息注入
     * 4.设置依赖功能可忽略的接口
     * 5.注册一些固定的依赖的属性
     * 6.增加 AspectJ 的支持（ 会在第7章中进行详细的讲解）
     * 7.将相关的环境变量及属性注册到以单例的模式注册
     * 可能读者不是很了解每个步骤的具体的含义，接下来我们对和个步骤进行详细的分析
     */
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        log.info("prepareBeanFactory beanFactory name :" + beanFactory.getClass().getName());
        // Tell the internal bean factory to use the context's class loader etc.
        //设置类加载器：存在则直接设置/不存在则新建一个默认类加载器 | 设置 beanFactory 的 classLoader 为当前的 context 的 classLoader
        beanFactory.setBeanClassLoader(getClassLoader());
        //设置EL表达式解析器（Bean初始化完成后填充属性时会用到） |
        //  设置 beanFactory 的表达式语言处理器，Spring 3 增强了表达式语言的支持
        //  默认可以使用#{bean.xxx} 的形式来调用相关的属性值
        // Spring 表达式语言全称"Spring Expression Language" 缩写"SpEL" ，类似于 Struts2.x 中使用的 OGNL 表达式语言，能在运行时构建
        // 复杂的表达式，存取对象图属性，对象方法调用等，并且能在 Spring  功能完美整合，比如能用来配置 bean 的定义，SPEL 是单独模块
        // 只依赖于 core模块，不依赖于其他的模块，可以单独使用
        // SPEL  使用#{...} 作为定界符，所以在大框号中字符中都将被该为是 SpEL ，使用格式如下：
        // <bean id="saxophone" value="com.xxx.xxx.xxx"/>
        // <bean>
        //      <property name="instrument" value="#{saxophone}">
        // </bean>
        //  相当于
        // <bean id="saxophone" value="com.xxx.xxx.xxx"/>
        // <bean>
        //      <property name="instrument" ref = "saxophone" />
        // <bean/>
        // 当然，上面只是列举了其中最简单的使用方式，SPEL功能强大，使用好了可以大大的提高开发效率，这里只是为了唤起读者的记忆来帮助我们
        //  理解源码，有兴趣的读者可以进一步的深入研究
        //  在源码中通过代码 beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
        // 注册语言解析器，就可以对 SPEL 进行解析了，那么注册解析器后Spring 又是在什么时候调用这个解析器进行解析的呢？
        /*	 * 之前我们讲解过 Spring 在 bean 进行初始化的时候会有一个属性填充一步，而这一步中，javax.swing.Spring 会调用 AbstractAutowireCapableBeanFactory
         * 类的 applyPropertValues  函数来完成功能，就是这个函数中，会通过构造 BeanDefinitionValueResolver 类型实例 valueResolver 来进行属性值
         * 的解析，同时，也是在这一步骤中一般通过 AbstractBeanFactory 中的 evaluateBeanDefinitionString 方法来完成 SPEL 解析
         * 当调用这个方法时会判断是否存在语言解析器，如果存在则调用语言解析器的方法进行解析，解析的过程就是在 Spring 的 expression 的包内
         * 这里不做过多的解析，我们通过查看 evaluateBeanDefinitionString 方法调用层次可以看出，应用语言解析器的调用主要是在解析依赖
         * 注入 bean 的时候，以及完成 bean 的初始化和属性获取后进行属性填充的时候
         */
        beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
        // 设置属性注册解析器PropertyEditor
        // 为 beanFactory 增加了一个默认的 propertyEditor，这个主要的对 bean 的属性等设置管理的一个工具
        // 6.5.2 增加属性注册编辑器
        // 在 Spring DI 注入的时候可以把普通的属性注入进来，但是像 Date类型就无法被识别了，例如：
        // public class UserManager {
        //  private Date dataValue;
        //  public Date getDateValue(){
        //      return dataValue;
        // }
        // public void setDataValue(Date dataValue){
        //      this.dataValue = dataValue;
        // }
        // public String toString(){
        //      return "dataValue" + dataValue;
        // }
        // }
        // 上面的代码中 ，需要对日期类型属性进行注入
        // <bean id="userManager" class="com.test.UserManager">
        //      <property name="dataValue">
        //          <value>2013-03-05</value>
        //      </property>
        //  </bean>
        // 测试代码
        // @Test
        // public void testDate(){
        //      ApplicationContext ctx = new ClassPathXmlApplicationContext("bean.xml");
        //      UserManager userManager = (UserManager) ctx.getBean("userManager");
        //      System.out.println(userManager);
        // }
        // 如果直接使用，则程序会报错，类型转换不成功，因为在 UserManager 中 dateValue 属性是 Date类型的，但是在 Xml 配置中
        //  却是 String  类型的，所以当然会报异常
        // Spring  针对经问题提供了两咱解决办法
        // 1. 使用自定义属性编辑器，通过继承 PropertyEditorSupport，重写 setAsText方法，具体的步骤如下：
        //  (1)编写自定义的属性编辑器
        // public class DatePropertyEditor extends PropertyEditorSupport{
        // private String formate = "";
        // public void setFormat(String format){
        //      this.formate = formate ;
        // }
        // public void setAsText(String arg0) throws IllegalArgumentException{
        //      System.out.println("arg0:" + arg0);
        //      SimpleDateFormat sdf = new SimpleDateFormat(format);
        //      try{
        //          Date d = sdf.parse(arg0);
        //          this.setValue(d);
        //      } catch(ParseException e ){
        //             e.printStackException();
        //      }
        // }
        //}
        // (2).将自定义属性编辑器注册到 Spring 中
        // <!--自定义属性编辑器-->
        // <bean class="org.Springframework.beans.factory.config.CustomEditorConfigurer">
        //      <property name="customEditors">
        //          <map>
        //              <entry key = "java.util.Date">
        //                  <bean class="com.test.DatePropertyEditor">
        //                       <property name="format" value="yyyy-MM--dd"/>
        //                  </bean>
        //              </entry>
        //          </map>
        //      </property>
        // </bean>
        // 配置文件中上引入类型为 org.springframework.beans.factory.config.CustomEditorConfigurer
        // 的 bean 并在属性 customEditors 中加入了自定义属性编辑器，其中Key 为属性编辑器所对应的类型，通过这样的
        // 配置，当 Spring 在注入 bean 的属性时一旦遇到 java.util.Date 类型属性会自动调用自定义的 DatePropertyEditor 解析器
        // 进行解析，并用解析结果代码配置属性进行注入
        /////////////////////////////////////////////////////////////////////////
        // 2. 注册 Spring自带的属性编辑器 CustomDataEditor
        // 通过注册 Spring 自带的属性编辑器 CustomDateEditor ，具体的步骤如下
        //(1) public class DatePropertyEditorRegistrar implements PropertyEditorRegistrar {
        //  public void registerCustomEditors (PropertyEditorRegistry registry ){
        //      registry.registerCustomEditor(Date.class,new CustomDateEditor(new SimpleDateFormate("yyyy-MM-dd"),true));
        // }
        // (2) 注册到 Spring 中
        // <!--注册到 Spring 自带的编辑器 	private PropertyEditorRegistrar[] propertyEditorRegistrars; -->
        // <bean class="org.Springframework.beans.factory.config.CustomEditorConfigurer">
        //      <property name="propertyEditorRegistrars">
        //          <list>
        //              <bean class="com.test.DatePropertyEditorRegistrar"/>
        //          </list>
        //      </property>
        //  </bean>
        // 我们通过配置文件中将自定义的 DatePropertyEditorRegistrar 注册进入org.springframework.bean.factory.config.CustomEditorConfigurer
        // 的 propertyEditorRegistrars 属性中，可以具有与方法同样的效果
        // 我们了解了自定义属性编辑器的使用，但是，似乎与本节围绕的核心代码 beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this,getEnvironment()))
        // 并无联系，因为在注册自定义属性编辑器的时候使用的是 PropertyEditorRegistry 的 registerCustomEditor 方法，而这里使用的是
        // ConfigurableListableBeanFactory 的 addPropertyEditorRegistrar 方法，我们妨深入探索一下 ResourceEditorRegistrar 的内部实现
        // 在 ResourceEditorRegistrar 中，我们最关心的方法就是  registerCustomEditors
        beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

        // Configure the bean factory with context callbacks.
        // 将当前的ApplicationContext对象交给ApplicationContextAwareProcessor类来处理，从而在Aware接口实现类中的注入applicationContext
        // 添加 BeanPostProcessor,
        // 对于这个方法的主要目的就是注册 BeanPostProcessor ，而真正的逻辑还是在 ApplicationContextAwareProcessor 中
        // ApplicationContextAwareProcessor 实现了 BeanPostProcessor接口，我们回顾下之前讲过的内容，在 Bean 初始化的时候
        // 也就是  Spring 激活 bean 的 init-method 的前后，会调用 beanPostProcessor 的 postProcessBeforeInitialization 方法和
        // postProcessAfterInitialization 方法
        // 同样，对于 ApplicationContextAwareProcessor 我们也关心这两个方法
        //  对于 postProcessAfterInitialization 方法，在 ApplicationContextAwareProcessor 中并没有做过多逻辑处理
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
        // 设置忽略自动装配的接口 | 设置几个忽略自动装配的接口
        // 当 Spring 将 ApplicationContextAwareProcessor 注册后，那么在 invokeAwareInterfaces 方法中间调用了
        // Aware 类已经不是普通的 bean 了，如 ResourceLoaderAware,ApplicationEventPublisherAware 等，那么当需要 Spring做
        // bean 的依赖注入的时候忽略它们，而在 ignoreDependencyInterface 的作用正是如此
        beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
        beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
        beanFactory.ignoreDependencyInterface(EnvironmentAware.class);

        // BeanFactory interface not registered as resolvable type in a plain factory.
        // MessageSource registered (and found for autowiring) as a bean.
        // 在普通的工厂中，BeanFactory接口并没有按照resolvable类型进行注册
        // MessageSource被注册成一个Bean（并被自动注入）
        // 注册可以解析的自动装配
        //BeanFactory.class为key，beanFactory为value放入到了beanFactory的resolvableDependencies属性中
        //resolvableDependencies是一个ConcurrentHashMap,映射依赖类型和对应的被注入的value
        //这样的话BeanFactory/ApplicationContext虽然没有以bean的方式被定义在工厂中，
        //但是也能够支持自动注入，因为他处于resolvableDependencies属性中 |
        // 设置几个自动装配的特殊规则
        //  当注册了依赖解析后，例如当注册了对 BeanFactory.class 解析之后，当 bean 的属性注入的时候，一旦检测到属性 beanFactory
        // 类型便会将 beanFactory 的实例注入进去
        beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
        //再将上下文的一些接口与上下文本身做映射，一一放入到resolvableDependencies中
        beanFactory.registerResolvableDependency(ResourceLoader.class, this);
        beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
        beanFactory.registerResolvableDependency(ApplicationContext.class, this);

        // Detect a LoadTimeWeaver and prepare for weaving, if found.
        // 如果当前BeanFactory包含loadTimeWeaver Bean，说明存在类加载期织入AspectJ，则把当前BeanFactory交给类加载期BeanPostProcessor实现类
        // LoadTimeWeaverAwareProcessor来处理，从而实现类加载期织入AspectJ的目的。
        // 1.跟踪进入，浅看下containsBean方法
        // 增加对 AspectJ 的支持
        if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
            //如果有LoadTimeWeaver，加入bean后处理器
            LogUtils.info("prepareBeanFactory beanFactory containsBean loadTimeWeaver ");
            // 在 AbstractApplicationContext 中的 prepareBeanFactory  函数是在容器初始化时候调用的，也就是说只有在注册了 LoadTimeWeaverAwareProcessor
            // 才会激活整个 AspectJ 的功能
            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
            // Set a temporary ClassLoader for type matching.
            // 为匹配类型设置一个临时的ClassLoader
            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
        }

        // Register default environment beans.
        // 注册默认的environment beans

        // 判断目前这个bean工厂中是否包含指定name的bean，忽略父工厂
        // 添加默认的系统环境 bean
        if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
            //虽然XmlWebApplicationContext中持有默认实现的StandardServletEnvironment
            //但是没有注册到beanFactory中，通过getEnvironment方法拿到持有的引用
            //2.注册environment单例
            ConfigurableEnvironment configurableEnvironment = getEnvironment();
            LogUtils.info("prepareBeanFactory beanFactory not contains environment, registerSingleton environment " + configurableEnvironment.getClass().getName());
            beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, configurableEnvironment);
        }
        if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
            //注册systemProperties单例
            Map<String, Object> maps = getEnvironment().getSystemProperties();
            LogUtils.info("prepareBeanFactory beanFactory not contains systemProperties, registerSingleton systemProperties  ");
            beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, maps);
        }
        if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
            //注册systemEnvironment单例
            Map<String, Object> maps = getEnvironment().getSystemEnvironment();
            LogUtils.info("prepareBeanFactory beanFactory not contains systemEnvironment , registerSingleton systemEnvironment ");
            beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, maps);
        }
    }


    /**
     * Modify the application context's internal bean factory after its standard
     * initialization. All bean definitions will have been loaded, but no beans
     * will have been instantiated yet. This allows for registering special
     * BeanPostProcessors etc in certain ApplicationContext implementations.
     *
     * @param beanFactory the bean factory used by the application context
     */
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        LogUtils.info("postProcessBeanFactory ", 5);
    }

    /**
     * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
     * respecting explicit order if given.
     * <p>Must be called before singleton instantiation.
     */
    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
    }

    /**
     * Instantiate and invoke all registered BeanPostProcessor beans,
     * respecting explicit order if given.
     * <p>Must be called before any instantiation of application beans.
     */
    protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
    }

    /**
     * Initialize the MessageSource.
     * Use parent's if none defined in this context.
     * 6.6.3 初始化消息资源
     * 在进行这段函数的解析之前，我们同样先来回顾一下 Spring 国际化的使用方法
     * 假设我们正在开发一个支持多国语言的 WEb 应用程序 , 要求系统能够根据客户端系统的语言类型返回对应的界面，英文的操作系统返回英文界面
     *  而中文的操作系统则返回中文的操作界面，这便是典型的 il8n 国际化问题，对于有国际化的要求的应用系统，我们不能简单的采用硬编码的方式编写
     *   用户界面信息，报错信息等内容，而必需为这些需要的信息进行特殊的处理，简单的来说，就是为了每种语言提供一套相应的资源文件，并以
     *   规范化命名的方式保存在特定的目录中，由系统自动的根据客户端语言选择适合的资源文件
     *
     * 国际化信息也称为本地化我就算，一般需要两个条件才可以确定一个特定的类型本地化信息，它们分别是语言类型，和国家/地区的类型，如中文本地
     * 化作息既中国大陆的地区中文，又有中国台湾地区的，中国香港地区的中文，还有新加坡地区的中文，java 通过 java.util.Locale 类表示一个
     * 本地对象，它允许通过语言参数和国家参数创建一个确定的本地对象
     * java.util.Locale 是表示语言和国家/地区信息的本地化类，它是创建国际化应用的基础，下面给出几个创建本地化对象的示例：
     * 1.带有语言和国家/地区信息的本地化对象
     * Locale locale1 = new Locale("zh","CN");
     * 2.只有语言信息的本地化对象
     * Locale locale3 = new Locale("zh");
     * 3.等同于 Locale("zh","CN");
     * 4.等同于 Locale("zh")
     * Locale locale4 = Locale.CHINESE;
     * 5.获取本地系统默认的本地化的对象
     * Locale locale5 = Locale.getDefault();
     * JDK 的 java.util 包中提供了几个支持本化格式的操作工具类，NumberFormate,DateFormat,MessageFormat ,而在 Spring 中
     * 国际化资源操作也无非是对于这些类的封装操作，我们仅仅介绍下 MessageFormat 的用法帮助大家回顾
     *
     *
     *
     *
     * 1.信息格式化串
     * String pattern1 = "{0} 你好！你于{1}在工商银行存入{2}元。"
     * String pattern2 = "At {1,time,short} On {1,date,long} ,{0} paid {2,number,currency}."
     * 2. 用于用户动态替换占位符的参数
     * Object[] params = {"John" ,new GregorianCalendar().getTime,1839288932}
     * 3.使用默认的本地化对象格式信息
     * String msg1 = MessageFormat.format(pattern1,params);
     *
     * 4.使用指定的本地批格式信息
     * MessageFormat mf = new MessageFormat (Pattern2,Locale.US);
     * String msg2 = mf.formate(params);
     * System.out.println(msg1);
     * System.out.println(msg2);
     * Spring 定义了访问国际化信息的 MessageSource 接口，并提供了几个易用的实现类，MessageSource 分别被 HierarchicalMessageSource
     * 和 ApplicationContext 接口扩展，这里主要看 HierarchicalMessageSource 接口的几个实现类
     *
     * HierarchicalMessageSource 接口最重要的两个实现类是 ResourceBundleMessageSource 和 ReloadableResourceBundleMessageSource
     * ,它们是基于 java 的 ResourceBundle 基础类实现，允许仅通过资源名加载国际化资源，ReloadableResourceBundleMessageSource
     * 提供了定时刷新的功能，允许在不重启系统的情况下，更新资源信息，StaticMessageSource 主要用于程序测试，它允许通过编程方式提供国际
     * 化信息，而 DelegatingMessageSource 是为了方便操作父 MessageSource 而提供的代理类，仅仅举例 ResourceBundleMessageSource 的实现方式
     * 定义资源文件
     * message.properties (默认：英文) , 内容仅一句，如下
     * test=test
     * message_zh_CN.properties(简体中文)
     * test=测试
     * 然后 cmd 打开命令行容器，输入 xxx ,转码一下
     * 2.定义配置文件
     * <bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource">
     *      <property name="basenames">
     *          <list>
     *              <value>/test/messages</value>
     *          </list>
     *      </property>
     * </bean>
     * 其中这个 beanId 的命名为 messageSource ，否则会抛出 NoSuchMessageException 异常
     * 使用，通过ApplicationContext 访问国际化信息
     * String [] configs = {"applicationContext.xml"}
     * ApplicationContext ctx = new ClassPathXmlApplicationContext(configs);
     * // 直接通过访问国际化信息
     * Object[] params = {"John",new GregorianCalendar().getTime()};
     * String str1 = ctx.getMessage("test",params,Locale.US);
     * String str2 = ctx.getMessage("test",params,Locale.CHINA);
     * System.out.println(str1);
     * System.out.println("str2");
     *
     * 在了解了 Spring国际化的使用后便可以进行源码的分析了
     * 在 initMessageSource中的方法主要功能是提取配置中定义的 messageSource ，并将其记录在 Spring 容器中，也就是 AbstractApplicationContext
     * 中，当然，如果用户未设置资源文件的话，Spring 中也提供了默认的配置 DelegatingMessageSource
     * 在 initMessageSource 中获取自定义资源的方式为 beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME,MessageSource.class) ,
     * 在这里 Sprng 使用了硬编码的方式硬性的规定了定义资源文件必需是 message ，否则便会获取不到自定义的资源文件的配置，这也是为什么之前
     * 提到的 Bean 的 Id 如果部位 message 会抛出异常
     *
     *  通过读取并将自定义的资源文件配置记录在容器中，那么就可以在获取资源文件的时候直接使用了，例如：在 AbstractApplicationContext
     *  中获取资源文件属性的方法
     *  public String getMessage(String code,Object args[] ,Locale locale ) throws NoSuchMessageException{
     *      return getMessageSource().getMessage(code,args,locale);
     *  }
     *  其中 getMessageSource()方法正是获取之前定义好的自定义资源配置信息
     */
    protected void initMessageSource() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        LogUtils.info("initMessageSource beanFactory :" + beanFactory.getClass().getName());
        if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {

            LogUtils.info("beanFactory containsLocalBean  messageSource");
            // 如果配置文件中配置了messageSource，那么将messageSource 提取并记录在 this.messageSource 中
            this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
            // Make MessageSource aware of parent MessageSource.
            if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
                HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
                if (hms.getParentMessageSource() == null) {
                    // Only set parent context as parent MessageSource if no parent MessageSource
                    // registered already.
                    hms.setParentMessageSource(getInternalParentMessageSource());
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Using MessageSource [" + this.messageSource + "]");
            }
        } else {
            LogUtils.info("initMessageSource else ");
            // Use empty MessageSource to be able to accept getMessage calls.
            // 如果用户并没有定义配置文件，那么使用临时的 DelegatingMessageSource 以便作为调用 getMessage 方法的返回
            DelegatingMessageSource dms = new DelegatingMessageSource();
            MessageSource messageSource = getInternalParentMessageSource();
            LogUtils.info("initMessageSource messageSource " + (messageSource == null ? messageSource : messageSource.getClass().getName()));
            dms.setParentMessageSource(messageSource);
            this.messageSource = dms;
            beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME +
                        "': using default [" + this.messageSource + "]");
            }
        }
    }

    /**
     * Initialize the ApplicationEventMulticaster.
     * Uses SimpleApplicationEventMulticaster if none defined in the context.
     *
     * @see SimpleApplicationEventMulticaster
     * 初始化 ApplicationEventMulticaster
     * 在讲解 Spring 的时候传播器之前，我们还是先来看一下 Spring 的事件监听的简单的用法
     * 定义监听事件
     * public class TestEvent extents ApplicationEvent {
     *
     *     public String msg ;
     *
     *     public TestEvent (Object source){
     *         super(source);
     *     }
     *
     *     public TestEvent(Object source ,String msg ){
     *         super(source);
     *         this.msg = msg ;
     *     }
     *
     *     public void print(){
     *         System.out.println(msg);
     *     }
     *
     * }
     *
     * 2.定义监听器
     * public class TestListener implements ApplicationListener {
     *     public void onApplicationEvent(ApplicationEvent event ){
     *         if(event instanceof TestEvent){
     *             TestEvent test = (TestEvent) event ;
     *             testEvent.print();
     *         }
     *     }
     * }
     * 3.添加配置文件
     * <bean id="testListener" class="com.test.event.TestListener"></bean>
     *
     * 4. 测试
     * public class Test(){
     *     public static void main(String [] args){
     *         ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
     *         TestEvent event = new TestEvent("hello","msg");
     *         context.publishEvent(event);
     *     }
     * }
     * 当程序运行时，Spring 会将发出的 TestEvent  事件给我们自定义的 TestListener 进行进一步的处理
     * 或者很多的人一下子会反映出设置模式中的观察者模式，这确实就是一个典型的应用，可以在此比较关心的事件结束后及时处理，那么我们要看看
     * ApplicationEventMulticaster 是如何被初始化的，以确保功能的正确性呢？
     * initApplicationEventMulticaster 的方式是比较简单的，无非是考虑两种情况
     * 如果用户自定义了事件广播器，那么使用用户自定义的事件广播器
     * 如果用户没有自定义事件广播器，那么使用默认的 ApplicationEventMulticaster 。
     *  按照之前介绍的顺序及逻辑，我们推断，作为广播器，一定要用于存在监听器并合适的时候调用监听器，那么我们不妨进行默认的
     *  广播器实现 SimpleApplicationEventMulticaster 来探究一下
     *
     */
    protected void initApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        LogUtils.info("initApplicationEventMulticaster beanFactory :" + beanFactory.getClass().getName());
        if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
            this.applicationEventMulticaster =
                    beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
            LogUtils.info("initApplicationEventMulticaster Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
        } else {
            this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
            beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
            LogUtils.info("initApplicationEventMulticaster Unable to locate ApplicationEventMulticaster with name '" +
                    APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
                    "': using default [" + this.applicationEventMulticaster + "]");
        }
    }

    /**
     * Initialize the LifecycleProcessor.
     * Uses DefaultLifecycleProcessor if none defined in the context.
     *
     * @see DefaultLifecycleProcessor
     * 当 ApplicationContext 启动或者停止时，它会通过 LifecycleProcessor 来与所有的声明的 bean 的周期做状态更新，
     * 而在 LifecycleProcessor 的使用前首先需要初始化
     *
     */
    protected void initLifecycleProcessor() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
            this.lifecycleProcessor =
                    beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
            if (logger.isDebugEnabled()) {
                logger.debug("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
            }
        } else {
            DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
            defaultProcessor.setBeanFactory(beanFactory);
            this.lifecycleProcessor = defaultProcessor;
            beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to locate LifecycleProcessor with name '" +
                        LIFECYCLE_PROCESSOR_BEAN_NAME +
                        "': using default [" + this.lifecycleProcessor + "]");
            }
        }
    }

    /**
     * Template method which can be overridden to add context-specific refresh work.
     * Called on initialization of special beans, before instantiation of singletons.
     * <p>This implementation is empty.
     *
     * @throws BeansException in case of errors
     * @see #refresh()
     */
    protected void onRefresh() throws BeansException {
        // For subclasses: do nothing by default.
        LogUtils.info("onRefresh ",5);
    }

    /**
     * Add beans that implement ApplicationListener as listeners.
     * Doesn't affect other listeners, which can be added without being beans.
     *  之前在介绍 Spring 广播时反复提到过事件监听器，那么在 Spring 注册监听器的时候，又做了哪些逻辑操作呢？
     *  硬编码的方式注册监听处理器
     *
     *
     *
     *   完成 BeanFactory 的初始化工作，其中包括 ConversionService 的设置，配置冰结以及非延迟加载的 bean 的初始化了工作了
     *
     *   冻结所有的 bean 的定义，说明注册的 bean 定义将不修改，
     *
     */
    protected void registerListeners() {
        // Register statically specified listeners first.
        for (ApplicationListener<?> listener : getApplicationListeners()) {
            LogUtils.info("listener Name :" + listener.getClass().getName());
            getApplicationEventMulticaster().addApplicationListener(listener);
        }

        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let post-processors apply to them!
        // 配置文件注册的监听器处理
        String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
        LogUtils.info("registerListeners listenerBeanNames :" + Arrays.toString(listenerBeanNames));
        for (String listenerBeanName : listenerBeanNames) {
            getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
        }

        // Publish early application events now that we finally have a multicaster...
        Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
        LogUtils.info("registerListeners earlyEventsToProcess :" + earlyEventsToProcess);
        this.earlyApplicationEvents = null;
        if (earlyEventsToProcess != null) {
            for (ApplicationEvent earlyEvent : earlyEventsToProcess) {

                getApplicationEventMulticaster().multicastEvent(earlyEvent);
            }
        }
    }

    /**
     * Finish the initialization of this context's bean factory,
     * initializing all remaining singleton beans.
     * 当Bean 定义资源被载入IoC容器之后，容器将Bean定义资源解析成容器内部的数据结构BeanDefinition，并注册到容器中，AbstractApplicationContext
     * 类中的finishBeanFactoryInitialization() 方法配置了预实例化属性的Bean进行预初始化如下：
     * 对配置了lazy-init属性进行预实例化处理
     *
     *  完成 BeanFactory 的初始化工作，其中包括 ConversionService 的设置，配置，冻结以及非延迟加载的 bean 的初始化工作
     *
     *  1.ConversionService 的设置
     *  之前我们提到过使用自定义类型转换器从 String 转换成 Date 的方式，那么 Spring 中还提供了另一种转换方式：使用 Converter
     *  同样，我们使用了一个简单的示例来了解一下 Converter 的使用方式
     *
     *  1. 定义转换器
     *  public class String2DateConverter implements Converter<String,Date> {
     * @Override
     *      public Date convert(String arg0){
     *          return DateUtils.parseDate(arg0,new String []{"yyyy-MM-dd HH:mm:ss"});
     *      }
     *  }
     *  2.注册
     *  <bean id="conversionService" class="org.Springframework.context.suppeort.ConversionServiceFactoryBean">
     *      <property name="converters">
     *           <list>
     *               <bean class="String2DateConverter"></bean>
     *           </list>
     *      </property>
     *  </bean>
     *
     * 3. 测试
     * 这样便可以使用 Converter 为我们提供功能了，下面我们通过一个简单的方法来对此直接测试
     * public void testStringToPhoneNumberConvert(){
     *     DefaultConversionService conversionService = new DefaultConversionService();
     *     conversionService.addConverter(new StringToPhoneNumberConverter());
     *     String phoneNumberStr = "010-12345678";
     *     PhoneNumberModel phoneNumber = conversionService.convert(PhoneNumberStr,PhoneNumberModel.class);
     *     Assert.assertEquals("010",phoneNumber.getAreaCode());
     * }
     * 这个例子有点奇怪
     * 通过以上的功能我们看到了 Converter 以及 ConversionService 提供了便利的功能，其中的配置就是在当前函数中被初始化的
     *
     */
    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        // Initialize conversion service for this context.
        // 这是Spring 3 新加的代码，为容器指定个转换服务（ConversionService）
        // 在对某些Bean属性进行转换时使用
        if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
                beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
            LogUtils.info("finishBeanFactoryInitialization beanFactory constains conversionService");
            beanFactory.setConversionService(
                    beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
        }

        // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
        String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
        LogUtils.info("finishBeanFactoryInitialization weaverAwareNames :" + Arrays.toString(weaverAwareNames));
        for (String weaverAwareName : weaverAwareNames) {
            getBean(weaverAwareName);
        }

        // Stop using the temporary ClassLoader for type matching.
        // 为了使类型匹配，停止使用临时的类加载器
        beanFactory.setTempClassLoader(null);

        // Allow for caching all bean definition metadata, not expecting further changes.
        // 缓存容器中所有的注册的BeanDefinition元数据，以防止被修改 | 冻结所有的 bean 的定义，说明注册的 bean 定义将不被修改
        // 或者进一步的处理
        beanFactory.freezeConfiguration();

        // Instantiate all remaining (non-lazy-init) singletons.
        // 对配置了lazy-init属性的单例模式的Bean进行预实例化处理 |  初始化剩下的单实例(非惰性的)
        beanFactory.preInstantiateSingletons();
    }

    /**
     * Finish the refresh of this context, invoking the LifecycleProcessor's
     * onRefresh() method and publishing the
     * {@link ContextRefreshedEvent}.
     * 在 Spring 中还提供了 Lifecycle接口，Lifecycle 中包含了 start/stop 方法，实现此接口后 Spring 还会保证在启动的时候调用其
     * start 方法开始生命周期，并在 Spring 关闭的时候调用stop 方法来结束生命周期，通常用来配置后台程序，在启动后一直运行
     *  如对 MQ 进行轮询等，而 ApplicationContext 在初始化最后正是保证了
     */
    protected void finishRefresh() {
        // Initialize lifecycle processor for this context.
        initLifecycleProcessor();

        // Propagate refresh to lifecycle processor first.
        // 启动所有实现了 Lifecycle 接口的 bean
        getLifecycleProcessor().onRefresh();

        // Publish the final event.
        publishEvent(new ContextRefreshedEvent(this));

        // Participate in LiveBeansView MBean, if active.
        LiveBeansView.registerApplicationContext(this);
    }

    /**
     * Cancel this context's refresh attempt, resetting the {@code active} flag
     * after an exception got thrown.
     *
     * @param ex the exception that led to the cancellation
     */
    protected void cancelRefresh(BeansException ex) {
        this.active.set(false);
    }

    /**
     * Reset Spring's common core caches, in particular the {@link ResolvableType}
     * and the {@link CachedIntrospectionResults} caches.
     *
     * @see ResolvableType#clearCache()
     * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
     * @since 4.2
     */
    protected void resetCommonCaches() {
        ResolvableType.clearCache();
        CachedIntrospectionResults.clearClassLoader(getClassLoader());
    }


    /**
     * Register a shutdown hook with the JVM runtime, closing this context
     * on JVM shutdown unless it has already been closed at that time.
     * <p>Delegates to {@code doClose()} for the actual closing procedure.
     *
     * @see Runtime#addShutdownHook
     * @see #close()
     * @see #doClose()
     */
    @Override
    public void registerShutdownHook() {
        if (this.shutdownHook == null) {
            // No shutdown hook registered yet.
            this.shutdownHook = new Thread() {
                @Override
                public void run() {
                    doClose();
                }
            };
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    /**
     * DisposableBean callback for destruction of this instance.
     * Only called when the ApplicationContext itself is running
     * as a bean in another BeanFactory or ApplicationContext,
     * which is rather unusual.
     * <p>The {@code close} method is the native way to
     * shut down an ApplicationContext.
     *
     * @see #close()
     * @see org.springframework.beans.factory.access.SingletonBeanFactoryLocator
     */
    @Override
    public void destroy() {
        close();
    }

    /**
     * Close this application context, destroying all beans in its bean factory.
     * <p>Delegates to {@code doClose()} for the actual closing procedure.
     * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
     *
     * @see #doClose()
     * @see #registerShutdownHook()
     */
    @Override
    public void close() {
        synchronized (this.startupShutdownMonitor) {
            doClose();
            // If we registered a JVM shutdown hook, we don't need it anymore now:
            // We've already explicitly closed the context.
            if (this.shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
                } catch (IllegalStateException ex) {
                    // ignore - VM is already shutting down
                }
            }
        }
    }

    /**
     * Actually performs context closing: publishes a ContextClosedEvent and
     * destroys the singletons in the bean factory of this application context.
     * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
     *
     * @see ContextClosedEvent
     * @see #destroyBeans()
     * @see #close()
     * @see #registerShutdownHook()
     */
    protected void doClose() {
        if (this.active.get() && this.closed.compareAndSet(false, true)) {
            if (logger.isInfoEnabled()) {
                logger.info("Closing " + this);
            }

            LiveBeansView.unregisterApplicationContext(this);

            try {
                // Publish shutdown event.
                publishEvent(new ContextClosedEvent(this));
            } catch (Throwable ex) {
                logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
            }

            // Stop all Lifecycle beans, to avoid delays during individual destruction.
            try {
                getLifecycleProcessor().onClose();
            } catch (Throwable ex) {
                logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
            }

            // Destroy all cached singletons in the context's BeanFactory.
            destroyBeans();

            // Close the state of this context itself.
            closeBeanFactory();

            // Let subclasses do some final clean-up if they wish...
            onClose();

            this.active.set(false);
        }
    }

    /**
     * Template method for destroying all beans that this context manages.
     * The default implementation destroy all cached singletons in this context,
     * invoking {@code DisposableBean.destroy()} and/or the specified
     * "destroy-method".
     * <p>Can be overridden to add context-specific bean destruction steps
     * right before or right after standard singleton destruction,
     * while the context's BeanFactory is still active.
     *
     * @see #getBeanFactory()
     * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
     */
    protected void destroyBeans() {
        getBeanFactory().destroySingletons();
    }

    /**
     * Template method which can be overridden to add context-specific shutdown work.
     * The default implementation is empty.
     * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
     * this context's BeanFactory has been closed. If custom shutdown logic
     * needs to execute while the BeanFactory is still active, override
     * the {@link #destroyBeans()} method instead.
     */
    protected void onClose() {
        // For subclasses: do nothing by default.
    }

    @Override
    public boolean isActive() {
        return this.active.get();
    }

    /**
     * Assert that this context's BeanFactory is currently active,
     * throwing an {@link IllegalStateException} if it isn't.
     * <p>Invoked by all {@link BeanFactory} delegation methods that depend
     * on an active context, i.e. in particular all bean accessor methods.
     * <p>The default implementation checks the {@link #isActive() 'active'} status
     * of this context overall. May be overridden for more specific checks, or for a
     * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
     */
    protected void assertBeanFactoryActive() {
        if (!this.active.get()) {
            if (this.closed.get()) {
                throw new IllegalStateException(getDisplayName() + " has been closed already");
            } else {
                throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
            }
        }
    }


    //---------------------------------------------------------------------
    // Implementation of BeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public Object getBean(String name) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        assertBeanFactoryActive();
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        LogUtils.info("getBean beanFactory name " + beanFactory.getClass().getName());
        return beanFactory.getBean(requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(requiredType, args);
    }

    @Override
    public boolean containsBean(String name) {
        return getBeanFactory().containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isSingleton(name);
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isPrototype(name);
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isTypeMatch(name, typeToMatch);
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().getType(name);
    }

    @Override
    public String[] getAliases(String name) {
        return getBeanFactory().getAliases(name);
    }


    //---------------------------------------------------------------------
    // Implementation of ListableBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return getBeanFactory().containsBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return getBeanFactory().getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
            throws BeansException {

        assertBeanFactoryActive();
        return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForAnnotation(annotationType);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
            throws BeansException {

        assertBeanFactoryActive();
        return getBeanFactory().getBeansWithAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
            throws NoSuchBeanDefinitionException {

        assertBeanFactoryActive();
        return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
    }


    //---------------------------------------------------------------------
    // Implementation of HierarchicalBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public BeanFactory getParentBeanFactory() {
        return getParent();
    }

    @Override
    public boolean containsLocalBean(String name) {
        return getBeanFactory().containsLocalBean(name);
    }

    /**
     * Return the internal bean factory of the parent context if it implements
     * ConfigurableApplicationContext; else, return the parent context itself.
     *
     * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
     */
    protected BeanFactory getInternalParentBeanFactory() {
        log.info(" getInternalParentBeanFactory " + getParent());
        if ((getParent() instanceof ConfigurableApplicationContext)) {
            log.info("getInternalParentBeanFactory instanceof ConfigurableApplicationContext");
            return ((ConfigurableApplicationContext) getParent()).getBeanFactory();
        } else {
            log.info("getInternalParentBeanFactory not instanceof ConfigurableApplicationContext");
            return getParent();
        }

    }


    //---------------------------------------------------------------------
    // Implementation of MessageSource interface
    //---------------------------------------------------------------------

    @Override
    public String getMessage(String code, Object args[], String defaultMessage, Locale locale) {
        return getMessageSource().getMessage(code, args, defaultMessage, locale);
    }

    @Override
    public String getMessage(String code, Object args[], Locale locale) throws NoSuchMessageException {
        return getMessageSource().getMessage(code, args, locale);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        return getMessageSource().getMessage(resolvable, locale);
    }

    /**
     * Return the internal MessageSource used by the context.
     *
     * @return the internal MessageSource (never {@code null})
     * @throws IllegalStateException if the context has not been initialized yet
     */
    private MessageSource getMessageSource() throws IllegalStateException {
        if (this.messageSource == null) {
            throw new IllegalStateException("MessageSource not initialized - " +
                    "call 'refresh' before accessing messages via the context: " + this);
        }
        return this.messageSource;
    }

    /**
     * Return the internal message source of the parent context if it is an
     * AbstractApplicationContext too; else, return the parent context itself.
     */
    protected MessageSource getInternalParentMessageSource() {
        return (getParent() instanceof AbstractApplicationContext) ?
                ((AbstractApplicationContext) getParent()).messageSource : getParent();
    }


    //---------------------------------------------------------------------
    // Implementation of ResourcePatternResolver interface
    //---------------------------------------------------------------------

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        return this.resourcePatternResolver.getResources(locationPattern);
    }


    //---------------------------------------------------------------------
    // Implementation of Lifecycle interface
    //---------------------------------------------------------------------

    @Override
    public void start() {
        getLifecycleProcessor().start();
        publishEvent(new ContextStartedEvent(this));
    }

    @Override
    public void stop() {
        getLifecycleProcessor().stop();
        publishEvent(new ContextStoppedEvent(this));
    }

    @Override
    public boolean isRunning() {
        return getLifecycleProcessor().isRunning();
    }


    //---------------------------------------------------------------------
    // Abstract methods that must be implemented by subclasses
    //---------------------------------------------------------------------

    /**
     * Subclasses must implement this method to perform the actual configuration load.
     * The method is invoked by {@link #refresh()} before any other initialization work.
     * <p>A subclass will either create a new bean factory and hold a reference to it,
     * or return a single BeanFactory instance that it holds. In the latter case, it will
     * usually throw an IllegalStateException if refreshing the context more than once.
     *
     * @throws BeansException        if initialization of the bean factory failed
     * @throws IllegalStateException if already initialized and multiple refresh
     *                               attempts are not supported
     */
    protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

    /**
     * Subclasses must implement this method to release their internal bean factory.
     * This method gets invoked by {@link #close()} after all other shutdown work.
     * <p>Should never throw an exception but rather log shutdown failures.
     */
    protected abstract void closeBeanFactory();

    /**
     * Subclasses must return their internal bean factory here. They should implement the
     * lookup efficiently, so that it can be called repeatedly without a performance penalty.
     * <p>Note: Subclasses should check whether the context is still active before
     * returning the internal bean factory. The internal factory should generally be
     * considered unavailable once the context has been closed.
     *
     * @return this application context's internal bean factory (never {@code null})
     * @throws IllegalStateException if the context does not hold an internal bean factory yet
     *                               (usually if {@link #refresh()} has never been called) or if the context has been
     *                               closed already
     * @see #refreshBeanFactory()
     * @see #closeBeanFactory()
     */
    @Override
    public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


    /**
     * Return information about this context.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getDisplayName());
        sb.append(": startup date [").append(new Date(getStartupDate()));
        sb.append("]; ");
        ApplicationContext parent = getParent();
        if (parent == null) {
            sb.append("root of context hierarchy");
        } else {
            sb.append("parent: ").append(parent.getDisplayName());
        }
        return sb.toString();
    }

}
