/*
 * Copyright 2002-2013 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;

import java.io.IOException;

/**
 * Base class for {@link ApplicationContext}
 * implementations which are supposed to support multiple calls to {@link #refresh()},
 * creating a new internal bean factory instance every time.
 * Typically (but not necessarily), such a context will be driven by
 * a set of config locations to load bean definitions from.
 *
 * <p>The only method to be implemented by subclasses is {@link #loadBeanDefinitions},
 * which gets invoked on each refresh. A concrete implementation is supposed to load
 * bean definitions into the given
 * {@link DefaultListableBeanFactory},
 * typically delegating to one or more specific bean definition readers.
 *
 * <p><b>Note that there is a similar base class for WebApplicationContexts.</b>
 * {@link org.springframework.web.context.support.AbstractRefreshableWebApplicationContext}
 * provides the same subclassing strategy, but additionally pre-implements
 * all context functionality for web environments. There is also a
 * pre-defined way to receive config locations for a web context.
 *
 * <p>Concrete standalone subclasses of this base class, reading in a
 * specific bean definition format, are {@link ClassPathXmlApplicationContext}
 * and {@link FileSystemXmlApplicationContext}, which both derive from the
 * common {@link AbstractXmlApplicationContext} base class;
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}
 * supports {@code @Configuration}-annotated classes as a source of bean definitions.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #loadBeanDefinitions
 * @see DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @since 1.1.3
 */
@Slf4j
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    private Boolean allowBeanDefinitionOverriding;

    private Boolean allowCircularReferences;

    /**
     * Bean factory for this context
     */
    private DefaultListableBeanFactory beanFactory;

    /**
     * Synchronization monitor for the internal BeanFactory
     */
    private final Object beanFactoryMonitor = new Object();


    /**
     * Create a new AbstractRefreshableApplicationContext with no parent.
     */
    public AbstractRefreshableApplicationContext() {
    }

    /**
     * Create a new AbstractRefreshableApplicationContext with the given parent context.
     *
     * @param parent the parent context
     */
    public AbstractRefreshableApplicationContext(ApplicationContext parent) {
        super(parent);
    }


    /**
     * Set whether it should be allowed to override bean definitions by registering
     * a different definition with the same name, automatically replacing the former.
     * If not, an exception will be thrown. Default is "true".
     *
     * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
     */
    public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
        this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
    }

    /**
     * Set whether to allow circular references between beans - and automatically
     * try to resolve them.
     * <p>Default is "true". Turn this off to throw an exception when encountering
     * a circular reference, disallowing them completely.
     *
     * @see DefaultListableBeanFactory#setAllowCircularReferences
     */
    public void setAllowCircularReferences(boolean allowCircularReferences) {
        this.allowCircularReferences = allowCircularReferences;
    }


    /**
     * This implementation performs an actual refresh of this context's underlying
     * bean factory, shutting down the previous bean factory (if any) and
     * initializing a fresh bean factory for the next phase of the context's lifecycle.
     * <p>
     * 初始化BeanFactory，并进行XML文件读取，并将得到的BeanFactory记录在当前实体的属性中
     * 在这个方法中，先判断beanFactory是否存在，如果存在，则先销毁Bean并关闭beanFactory,接着创建DefaultListableBeanFactory
     * ，并调用loadBeanDefinitions()方法装载bean的定义
     *
     *
     *
     * 1.我们详细分析下面代码的步骤
     * 在介绍BeanFactory 的时候，不知道读者是不是有印象，声明方式 BeanFactory bf = new XmlBeanFactory("beanFactoryTest.xml");
     * 其中 XmlBeanFactory 继承 DefaultListableBeanFactory ，并提供了 XmlBeanDefinitionReader 类型的 reader 属性，也就是说
     * DefaultListableBeanFactory 是容器的基础，必需首先要实例化，那么在这里就是实例化 DefaultListableBeanFactory 的步骤
     *
     * 2. 指定序列化 id
     * 3.定制 BeanFactory
     * 4.加载 beanDefinition
     * 5.使用全局变量记录 BeanFactory 类的实例
     * 因为 DefaultListableBeanFactory 类型的变量 beanFactory 是函数内部的局部变量，所以要使用全局变量记录解析结果
     *
     */
    @Override
    protected final void refreshBeanFactory() throws BeansException {
        log.info(" refreshBeanFactory ");
        // 如果已经存在容器，销毁容器中的Bean,关闭容器
        if (hasBeanFactory()) {
            log.info(" destroyBeans ");
            destroyBeans();
            log.info(" closeBeanFactory ");
            closeBeanFactory();
        }
        try {
            //
            log.info(" start createBeanFactory ");
            // 创建DefaultListableBeanFactory,创建IOC容器
            DefaultListableBeanFactory beanFactory = createBeanFactory();
            String id = getId();
            log.info(" end createBeanFactory  " + beanFactory.getClass().getName() + "  id = " + id);
            // 为了序列话指定id，如果需要的话，让这个BeanFactory从id反序列化到BeanFactory对象

            beanFactory.setSerializationId(id);
            /**
             * 设置两个属性：
             * 1. 是否允许覆盖同名称的不同定义的对象
             * 2. 是否允许bean之间存在循环依赖
             * 对Ioc容器进行定制化，如果设置了启动参数，开启注解的自动装配等
             * 设置@Autowired 和@Qualifier 注解解析器 QualifierAnnotaionAutowireCandidateResolver
             */
            log.info(" start customizeBeanFactory ");
            customizeBeanFactory(beanFactory);
            log.info(" end customizeBeanFactory ");
            // 初始化DocumentReader，并进行XML文件读取和解析
            // 调用载入的Bean定义的方法，这里又使用了一个委派模式
            // 当前类中只定义了一个抽象的loadBeanDefinitions()方法，调用子类容器实现
            loadBeanDefinitions(beanFactory);

            synchronized (this.beanFactoryMonitor) {
                this.beanFactory = beanFactory;
            }
        } catch (IOException ex) {
            throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
        }
    }

    @Override
    protected void cancelRefresh(BeansException ex) {
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory != null)
                this.beanFactory.setSerializationId(null);
        }
        super.cancelRefresh(ex);
    }

    @Override
    protected final void closeBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            this.beanFactory.setSerializationId(null);
            this.beanFactory = null;
        }
    }

    /**
     * Determine whether this context currently holds a bean factory,
     * i.e. has been refreshed at least once and not been closed yet.
     */
    protected final boolean hasBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            return (this.beanFactory != null);
        }
    }

    @Override
    public final ConfigurableListableBeanFactory getBeanFactory() {
        log.info("getBeanFactory");
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory == null) {
                throw new IllegalStateException("BeanFactory not initialized or already closed - " +
                        "call 'refresh' before accessing beans via the ApplicationContext");
            }
            return this.beanFactory;
        }
    }

    /**
     * Overridden to turn it into a no-op: With AbstractRefreshableApplicationContext,
     * {@link #getBeanFactory()} serves a strong assertion for an active context anyway.
     */
    @Override
    protected void assertBeanFactoryActive() {
    }

    /**
     * Create an internal bean factory for this context.
     * Called for each {@link #refresh()} attempt.
     * <p>The default implementation creates a
     * {@link DefaultListableBeanFactory}
     * with the {@linkplain #getInternalParentBeanFactory() internal bean factory} of this
     * context's parent as parent bean factory. Can be overridden in subclasses,
     * for example to customize DefaultListableBeanFactory's settings.
     *
     * @return the bean factory for this context
     * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
     * @see DefaultListableBeanFactory#setAllowEagerClassLoading
     * @see DefaultListableBeanFactory#setAllowCircularReferences
     * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
     */
    protected DefaultListableBeanFactory createBeanFactory() {
        BeanFactory beanFactory = getInternalParentBeanFactory();
        log.info("DefaultListableBeanFactory  createBeanFactory BeanFactory =  {}  ", beanFactory);
        return new DefaultListableBeanFactory(beanFactory);
    }

    /**
     * Customize the internal bean factory used by this context.
     * Called for each {@link #refresh()} attempt.
     * <p>The default implementation applies this context's
     * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
     * and {@linkplain #setAllowCircularReferences "allowCircularReferences"} settings,
     * if specified. Can be overridden in subclasses to customize any of
     * {@link DefaultListableBeanFactory}'s settings.
     *
     * @param beanFactory the newly created bean factory for this context
     * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
     * @see DefaultListableBeanFactory#setAllowCircularReferences
     * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
     * @see DefaultListableBeanFactory#setAllowEagerClassLoading
     * 这里已经开始对 BeanFactory 的扩展，在基本容器的基础上，增加了是否允许覆盖是否允许扩展设置并提供注解
     * @Qualifier 和@Autowired 的支持
     * 对于允许覆盖和允许依赖的设置这里是判断是否为空，如果不为空要进行设置，但是并没有看到哪里进行设置，究竟这个设置是在哪里进行的
     * 呢？还是那句话，使用子类覆盖方法:例如：
     * public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext{
     *     ... ...
     *     protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory){
     *         super.setAllowBeanDefinitionOverriding(false);
     *         super.setAlloCircularReferences(false);
     *         super.customizeBeanFactory(beanFactory);
     *     }
     * }
     *  设置完后相信大家已经对这两个属性的使用有所了解，或者可以回到前面的章节进行再一次查看，对于定制 BeanFactory ，Spring 还提供了
     *  另外一个重要的扩展，就是设置 AutowireCandidateResolver，在 Bean 加载部分讲解创建 bean 时，如果采用了 autowireByType 方式
     *  注入，那么会默认使用 Spring 提供的 SimpleAutowireCandidateResolver，而对于默认的实现并没有过多逻辑处理，在这里，Spring  使用了
     *  QualifereAnnotationAugowireCanDidateResolver，设置这个解析后的 Spring 就可以支持注解方式注入了
     *  Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
     *  因此，我们知道，在 QualifierAnnotationAutowireCandidateResolver 中一定会提供了解析 Qualifier 与 Autowire 注解的方法
     *  
     */
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        // 如果属性allowBeanDefinitionOverriding不为空，设置给beanFactory对象相应属性
        // 此属性的含义：是否允许覆盖同名称的不同定义的对象
        log.info(" allowBeanDefinitionOverriding = " + this.allowBeanDefinitionOverriding);
        if (this.allowBeanDefinitionOverriding != null) {
            beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
        }
        // 如果属性allowCircularReferences不为空，设置给beanFactory对象相应属性
        // 此属性的含义：是否允许bean之间存在循环依赖
        log.info(" allowCircularReferences = " + this.allowCircularReferences);
        if (this.allowCircularReferences != null) {
            beanFactory.setAllowCircularReferences(this.allowCircularReferences);
        }
    }

    /**
     * Load bean definitions into the given bean factory, typically through
     * delegating to one or more bean definition readers.
     *
     * @param beanFactory the bean factory to load bean definitions into
     * @throws BeansException if parsing of the bean definitions failed
     * @throws IOException    if loading of bean definition files failed
     * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
     * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
     */
    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
            throws BeansException, IOException;

}
