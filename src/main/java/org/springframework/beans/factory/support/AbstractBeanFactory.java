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

package org.springframework.beans.factory.support;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.test.LogUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Abstract base class for {@link org.springframework.beans.factory.BeanFactory}
 * implementations, providing the full capabilities of the
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI.
 * Does <i>not</i> assume a listable bean factory: can therefore also be used
 * as base class for bean factory implementations which obtain bean definitions
 * from some backend resource (where bean definition access is an expensive operation).
 *
 * <p>This class provides a singleton cache (through its base class
 * {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry},
 * singleton/prototype determination, {@link org.springframework.beans.factory.FactoryBean}
 * handling, aliases, bean definition merging for child bean definitions,
 * and bean destruction ({@link org.springframework.beans.factory.DisposableBean}
 * interface, custom destroy methods). Furthermore, it can manage a bean factory
 * hierarchy (delegating to the parent in case of an unknown bean), through implementing
 * the {@link org.springframework.beans.factory.HierarchicalBeanFactory} interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * {@link #getBeanDefinition} and {@link #createBean}, retrieving a bean definition
 * for a given bean name and creating a bean instance for a given bean definition,
 * respectively. Default implementations of those operations can be found in
 * {@link DefaultListableBeanFactory} and {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @since 15 April 2001
 * @see #getBeanDefinition
 * @see #createBean
 * @see AbstractAutowireCapableBeanFactory#createBean
 * @see DefaultListableBeanFactory#getBeanDefinition
 * 当Spring IoC容器完成了Bean定义资源的定位，载入和解析注册，IoC容器就可以管理Bean定义相关的数据了，但是此时IoC容器还没有对所管理的Bean
 * 进行依赖注入，依赖注入在下面的两种情况下发生
 * 1.用户第一次使用getBean()方法时，Ioc容器触发依赖注入
 * 2.当用户在配置文件中将<bean>元素配置了lazy-init=false属性时，即让容器在解析注册Bean定义时进行预实例化，触发依赖注入
 * BeanFactory接口定义了Spring Ioc容器的基本功能规范，是Spring Ioc容器所就遵守的最低层和最基本的编程规范，BeanFactory接口中定义了
 * 几个getBean()方法，用于用户向Ioc容器索取 被管理的Bean方法，我们通过分析其子类的具体的实现来理解 Spring Ioc容器时如何完成依赖注入
 * 在BeanFactory中我们可以看到getBean(String ..)方法，但是具体的实现在AbstractBeanFactory中
 * SimpleAliasRegistry<--DefaultSingletonBeanRegistry<--FactoryBeanRegistrySupport<--AbstracBeanFactory<--AbstractAutowireCapableBeanFactory
 * <--DefaultListableBeanFactory
 */

public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

	/** Parent bean factory, for bean inheritance support */
	private BeanFactory parentBeanFactory;

	/** ClassLoader to resolve bean class names with, if necessary */
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** ClassLoader to temporarily resolve bean class names with, if necessary */
	private ClassLoader tempClassLoader;

	/** Whether to cache bean metadata or rather reobtain it for every access */
	private boolean cacheBeanMetadata = true;

	/** Resolution strategy for expressions in bean definition values */
	private BeanExpressionResolver beanExpressionResolver;

	/** Spring ConversionService to use instead of PropertyEditors */
	private ConversionService conversionService;

	/** Custom PropertyEditorRegistrars to apply to the beans of this factory */
	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars =
			new LinkedHashSet<PropertyEditorRegistrar>(4);

	/** A custom TypeConverter to use, overriding the default PropertyEditor mechanism */
	private TypeConverter typeConverter;

	/** Custom PropertyEditors to apply to the beans of this factory */
	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors =
			new HashMap<Class<?>, Class<? extends PropertyEditor>>(4);

	/** String resolvers to apply e.g. to annotation attribute values */
	private final List<StringValueResolver> embeddedValueResolvers = new LinkedList<StringValueResolver>();

	/** BeanPostProcessors to apply in createBean */
	private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

	/** Indicates whether any InstantiationAwareBeanPostProcessors have been registered */
	private boolean hasInstantiationAwareBeanPostProcessors;

	/** Indicates whether any DestructionAwareBeanPostProcessors have been registered */
	private boolean hasDestructionAwareBeanPostProcessors;

	/** Map from scope identifier String to corresponding Scope */
	private final Map<String, Scope> scopes = new LinkedHashMap<String, Scope>(8);

	/** Security context used when running with a SecurityManager */
	private SecurityContextProvider securityContextProvider;

	/** Map from bean name to merged RootBeanDefinition
	 * 由于BeanDefinition有各种实现类，且实现类能指定'父子'依赖信息的关系(不是继承关系，
	 * 仅仅是指定数据的父子关系)。如果元数据依赖不合并的话，则这个beanDefinition的元数据信息将不完整。
	 *
	 * applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);，
	 * 应用合并BeanDefinition的后置处理器此方法主要缓存一些带了注解的属性字段例如@Autowired、@Common等元数据注入信息*/
	private final Map<String, RootBeanDefinition> mergedBeanDefinitions =
			new ConcurrentHashMap<String, RootBeanDefinition>(64);

	/** Names of beans that have already been created at least once */
	private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(64));

	/** Names of beans that are currently in creation */
	private final ThreadLocal<Object> prototypesCurrentlyInCreation =
			new NamedThreadLocal<Object>("Prototype beans currently in creation");


	/**
	 * Create a new AbstractBeanFactory.
	 */
	public AbstractBeanFactory() {
	}

	/**
	 * Create a new AbstractBeanFactory with the given parent.
	 * @param parentBeanFactory parent bean factory, or {@code null} if none
	 * @see #getBean
	 */
	public AbstractBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	// 获取IoC容器中的指定名称的Bean
	@Override
	public Object getBean(String name) throws BeansException {
		// doGetBean才是真正的向Ioc容器中获取管理的Bean
		return doGetBean(name, null, null, false);
	}

	// 获取Ioc容器中指定名称和类型的Bean
	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		// doGetBean才是真正的向Ioc容器中获取被管理的Bean
		return doGetBean(name, requiredType, null, false);
	}

	// 获取IoC容器中指定名称和参数的Bean
	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		// doGetBean才是真正的向容器获取被管理的Bean
		return doGetBean(name, null, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * @param name the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 * (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 * 获取Ioc容器中指定名称，类型和参数的Bean
	 */
	public <T> T getBean(String name, Class<T> requiredType, Object... args) throws BeansException {
		// doGetBean才是真正的向Ioc容器中获取被管理的Bean
		return doGetBean(name, requiredType, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * @param name the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 * (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @param typeCheckOnly whether the instance is obtained for a type check,
	 * not for actual use
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 * 真正的实现向Ioc容器获取Bean的功能，也是就触发依赖注入的地方
	 * 通过对IOC容器的获取Bean的的分析，我们可以看到，在Spring中，如果Bean定义为单例模式（Singleton）的，容器在创建之前先从缓存中查找
	 * 以确保整个容器中只存在一个实例对象时，如果Bean定义为原型模式，则容器每次都会创建一个新的的实例对象，除此之外，Bean定义还可以指定其生命
	 * 周期范围
	 * 上面的源码只定义了根据Bean定义的不同模式采取的创建Bean实例对象的不同策略，具体的Bean实例对象创建过程由实现了ObjectFactory接口匿名
	 * 内部类createBean()方法完成，ObjectFactory接口使用了委派模式，具体的Bean实例创建过程交由其实现类AbstractAutowireCapableBeanFactory
	 * 完成，下面我们继续分析AbstractAutowireCapableBeanFactory的createBean()方法的源码，理解 创建Bean实例 具体的过程
	 *
	 *
	 *
	 * Spring 加载 bean 的过程
	 * 1.转换对应的 beanName
	 * 2.或许很多的人不理解转换对应的 beanName 是什么意思，传入的参数 name 就是 beanName 吗？其实不是，这个传入的参数可能是别名，也可能是
	 *   其实不是，这里传入的参数可能是别名，也可能是 FactoryBean,所以需要进行一系列的解析，这些解析的内容包括如下内容
	 *   去除 FactoryBean 的修饰符，也就是如果 name="&aa" ，那么会首先去除&而使得 name = "aa" ，
	 *   取指定的 alias 所表示的最终的 beanName ,例如别名 A 指向别名 B的 bean 则返回 B ;若别名 A 指向别名 B,别名 B 又指向别名 C的Bean
	 *   则直接返回 C
	 *   单例的在 Spring 的同一个容器内只会被创建一次，后续再获取 Bean ,就直接从单例缓存中获取了，当然，这里也只是尝试加载，首先会尝试
	 *   从缓存中加载，如果加载不成功，则尝试从 singletonFactorys 中加载，因为为创建单例 bean 的时候会存在依赖注入的情况，而在创建依赖的时候
	 *   为了避免循环依赖，在 Spring 中创建 Bean 的原则是不等 bean 创建完成就会将创建的 bean 的 ObjectBeanFactory 提前曝光加入到缓存中的
	 *   一旦下一个 bean 创建的时候需要依赖上一个 bean 则直接使用BeanFactory
	 * 3. bean 的实例化
	 *   如果从缓存中得到 bean 的原始状态，则需要对 bean 进行实例化，这里有必要强调一下, 缓存中的记录只是原始的 bean 的状态,并不一定是我们
	 *    最终想要的 bean ，举个例子，但是我们真正需要的是工厂 bean 中定义的 factory-method 方法中返回的 bean，而 getObjectForBeanInstance 就完成
	 *    了这个工作，后续会详细的讲解
	 *  4.原型模式的依赖检查
	*  只有在单例的情况下，才会尝试解决的循环依赖，如果存在 A中有 B 的属性，B 中有 A 的属性，那么当依赖注入的时候，就会产生 A 还未创建
	 *   完成的时候，因为对于 B 的创建再次返回创建 A ,造成循环依赖，也就是情况,isPrototypeCurrentlyInCreation(beanName) 判断 true
	 *   检测 parentBeanFatory
	 *   从代码的上来看，如果缓存 没有数据的话，直接转到父类的工厂上去加载了，这是为什么呢？
	 *   可能读者会忽略一个很重要的判断条件，parentBeanFactory !=null && !containsBeanDefinition(beanName) ,parentBeanFactory !=null
	 *   ,parentBeanFactory !=null ,parentBeanFactory  如果为空，则其他的一切都是浮云，但是!containsBeanDefinition(beanName) 就比较
	 *   重要了，它是在检测如果当前加载的 xml 配置文件中不包含 beanName 所对应的配置，就只能到 parentBeanFactory 去尝试一下，然后再去
	 *   递归的调用 getBean 方法
	 *  6.将存储的 xml 配置文件中的 GernericBeanDefinition 转换成 RootBeanDefinition 中的，但是所有的 Bean后续处理都是针对
	 *  RootBeanDefintion的，所以这里需要转换，转换的同时如果父类 bean 不为空的话，则会一并合并父类的属性
	 *
	 *  7.寻找依赖，因为 bean 初始化的过程很可能是会用到某些属性的，而某些属性很可能是动态配置的，并且配置成依赖于其他的 bean , 那么
	 *  这个时候就有必要加载依赖的bean ，那么这个时候就有必要先加载依赖的 bean ，所以，在 Spring 的加载顺序中，在初始化某一个 bean 的时候
	 *  首先会初始化这个 bean 所对应的依赖
	 *  8.针对不同的 scope 进行 bean 的创建
	 *  我们都知道，在 Spring 中存在着不同的 scope ，其中默认的是 singleton ，但是还有一些其他的配置，诸如 prototype ，request 之类的
	 *  在这个步骤中，Spring会根据不同的配置进行不同的初始化策略
	 *
	 *  9. 程序到这里返回 bean后已经基本的结果了，通常对该方法的调用参数 requiredType 是为空的，但是可能会存在这样的一种情况，返回的 bean
	 *  其实是 String 类型的，但是 requiredType 传入的是 Integer类型，那么这个时候本步骤会直到了作用，它的功能是将返回bean 转换成
	 *  requiredtype 所指定的类型，当然，String  转换为 Integer 是最简单的一种转换，在 Spring 提供了各种各样的转换器，用户也可以自己定义
	 *  自己的转换器来满足需求
	 *  经过上面的步骤后bean 的加载已经结束了，这个时候就可以返回我们需要的 bean 了，直观的反映整个过程，其中最重要的步骤就是8，针对不同的
	 *  scope 进行 bean 的创建，你会看到各种常用的 Spring中提供了各种各样的转换器，用户也可以自己扩展转换器来满足需求
	 *
	 *  经过上面的步骤后，bean 的加载已经结束，这个时候就可以返回我们所需要的 bean 了，
	 *
	 *
	 */
	@SuppressWarnings("unchecked")
	protected <T> T doGetBean(
			final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
			throws BeansException {
		//根据指定的名称获取被管理的Bean的名称，剥离指定的名称中对容器的相关依赖
		// 如果指定的是别名，将别名转换成规范的Bean的名称
		final String beanName = transformedBeanName(name);
		Object bean;
		// Eagerly check singleton cache for manually registered singletons.
		// 检查缓存中或者实例工厂中是否有对应的实例
		//  为什么首先会使用这段代码呢？
		// 因为在创建单例 bean 的时候会存在依赖注入的情况，而在创建依赖的时候，为了避免循环依赖
		// Spring 创建 bean 的原则是不等 bean 的创建完成就会将创建的 bean  的 ObjectFactory 提早曝光
		// 也就是将 ObjectBeanFactory  加入到缓存中，一旦下个 bean 创建的时候需要依赖上个 bean 则直接使用 ObjectFactory
		// 直接尝试从缓存中获取或者 singletonFactories 中的 ObjectFactory 中获取
		Object sharedInstance = getSingleton(beanName);
		LogUtils.info("doGetBean sharedInstance :" + sharedInstance + " ,beanName="+beanName + ",args="+Arrays.toString(args));
		// 先从缓存 中读取是否已经有被创建过的单例模式的bean
		// 对于单例模式的Bean,整个Ioc容器只创建一次，不需要重复的创建
		if (sharedInstance != null && args == null) {
			if (isSingletonCurrentlyInCreation(beanName)) {
				//如果在容器中已经有指定名称的单例模式的Bean,被创建，直接返回已经创建好的Bean
				LogUtils.info("doGetBean Returning eagerly cached instance of singleton bean '" + beanName +
						"' that is not fully initialized yet - a consequence of a circular reference");
			}else {
				LogUtils.info("doGetBean Returning cached instance of singleton bean '" + beanName + "'");
			}
			// 获取给定的Bean的实例对象，主要完成FactoryBean相关处理
			// 注意：BeanFactory是管理Bean的工厂，FactoryBean是创建对象的工厂Bean，两者之间是有很多的区别的 | 返回对应的实例
			// 有个时候存在诸如 BeanFactory 的情况并不是直接返回实例本身而是返回指定方法返回的实例
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		} else {
			//缓存中已经在有原型模式的Bean
			// 但是由于循环引用导致实例化对象失败
			LogUtils.info("doGetBean sharedInstance is null beanName "  +beanName ,5);
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.
			// 只有在单例的情况下都会以尝试解析循环依赖，原型模式情况下，如果存在 A 中有 B 的属性，B中有 A的属性，那么当依赖
			//  注入的时候，就会产生当 A 还示创建完成的时候因为
			// 对 B 的创建再次返回创建 A ,造成我一依赖，也就是下面的情况
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			// 对于Ioc容器中是否存在指定名称的BeanDefinition进行检查，首先检查是否
			// 能在当前的BeanFactory中获取所需要的Bean,如果不能则委托当前的容器
			// 的父容器去查找 ，如果还是找不到，则沿着容器的继承体系向父容器中查找
			BeanFactory parentBeanFactory = getParentBeanFactory();
			//当前容器的父容器存在，且当前容器中不存在指定的名称的Bean  |  如果 beanDefinitionMap 中也就是在所有的已经加载的类中不包括
			// beanName，则尝试从 parentBeanFactory 中检测
			LogUtils.info("doGetBean parentBeanFactory Name : "   + (parentBeanFactory !=null ? parentBeanFactory.getClass().getName() :null));
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				// 解析指定的Bean名称的原始名称
				String nameToLookup = originalBeanName(name);
				LogUtils.info("doGetBean nameToLookup  name : "   + name);
				// 递归到 BeanFactory 中查找
				if (args != null) {
					// Delegation to parent with explicit args.
					// 委派父容器根据指定名称和显示的参数查找
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				}
				else {
					// No args -> delegate to standard getBean method.
					// 委派父容器根据指定名称和类型查找
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
			}
			LogUtils.info("doGetBean typeCheckOnly   : "   + typeCheckOnly);
			//创建的Bean是否需要进行类型验证，一般是不需要的 | 如果不是仅仅做类型检查则是创建 bean,这里需要进行记录
			if (!typeCheckOnly) {
				// 向容器标记指定的Bean是否已经被创建
				LogUtils.info("doGetBean markBeanAsCreated   : "   + beanName);
				markBeanAsCreated(beanName);
			}

			try {
				//根据指定的Bean的名称获取其父级别的Bean的定义
				// 主要解决Bean继承子类和父类公共属性的问题，将存在 XML 配置文件的 GernericBeanDefinition 转换为 RootBeanDefinition
				// 如果指定 BeanName 是子的 Bean 的话同时合并父类的相关属性
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				// 获取当前Bean所依赖的Bean的名称 |    如果存在依赖则需要递归实例化依赖的 bean
				String[] dependsOn = mbd.getDependsOn();
				//如果当前的Bean有依赖的Bean
				if (dependsOn != null) {
					for (String dependsOnBean : dependsOn) {
						if (isDependent(beanName, dependsOnBean)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dependsOnBean + "'");
						}
						//把被依赖的Bean注册给当前的依赖的Bean | 缓存依赖调用
						registerDependentBean(dependsOnBean, beanName);
						// 递归调用getBean()方法，获取当前的Bean的依赖的Bean
						getBean(dependsOnBean);
					}
				}

				// Create bean instance.
				// 创建单例模式的Bean的实例对象
				if (mbd.isSingleton()) {
					//这里使用了一个匿名的内部类创建Bean实例对象，并且注册给所依赖的对象
					sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
						@Override
						public Object getObject() throws BeansException {
							try {
								//创建一个指定的Bean的实例对象，如果有父类继承，则合并子类和父类的定义
								return createBean(beanName, mbd, args);
							}
							catch (BeansException ex) {
								// Explicitly remove instance from singleton cache: It might have been put there
								// eagerly by the creation process, to allow for circular reference resolution.
								// Also remove any beans that received a temporary reference to the bean.
								// 显示的从容器中单例模式的Bean缓存中清除实例对象
								destroySingleton(beanName);
								throw ex;
							}
						}
					});
					//获取给定的Bean的实例对象
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}
				//Ioc容器创建原型模式的Bean的实例对象
				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					// 原型模式（Prototype）每次都会创建一个新的对象
					Object prototypeInstance = null;
					try {
						//回调BeforePrototypeCreation()方法，默认的功能是在注册当前创建的原型对象
						beforePrototypeCreation(beanName);
						//创建指定的Bean的对象实例
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						//回调afterPrototypeCreation()方法，默认的功能是告诉Ioc容器不要再创建指定的Bean的原型对象
						afterPrototypeCreation(beanName);
					}
					//获取指定的Bean的实例对象
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}
				//如果要创建的对象既不是单例模式，也不是原型模式，则根据Bean定义资源中
				// 配置的生命周期范围，选择实例化Bean的合法方法，这种方法在WEb应用程序中
				//比较常用，如request,session,application等生命周期
				else {
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope '" + scopeName + "'");
					}
					try {
						// 这里又使用了一个匿名的内部类，获取 一个指定的生命周期的实例
						Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
							@Override
							public Object getObject() throws BeansException {
								beforePrototypeCreation(beanName);
								try {
									return createBean(beanName, mbd, args);
								}
								finally {
									afterPrototypeCreation(beanName);
								}
							}
						});
						// 获取指定的Bean的实例对象
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; " +
								"consider defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			}
			catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		// 对创建Bean实例对象进行检查 | 检查需要的类型是否符合 bean 的实际类型
		if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
			try {
				return getTypeConverter().convertIfNecessary(bean, requiredType);
			}
			catch (TypeMismatchException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to convert bean '" + name + "' to required type [" +
							ClassUtils.getQualifiedName(requiredType) + "]", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

	@Override
	public boolean containsBean(String name) {

		LogUtils.info("containsBean name " + name,3);
		String beanName = transformedBeanName(name);

		LogUtils.info("containsBean beanName : " + beanName);
		if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
		}
		// Not found -> check parent.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean) {
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			}
			else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		}
		else if (containsSingleton(beanName)) {
			return true;
		}

		else {
			// No singleton instance found -> check bean definition.
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// No bean definition found in this factory -> delegate to parent.
				return parentBeanFactory.isSingleton(originalBeanName(name));
			}

			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			// In case of FactoryBean, return singleton status of created object if not a dereference.
			if (mbd.isSingleton()) {
				if (isFactoryBean(beanName, mbd)) {
					if (BeanFactoryUtils.isFactoryDereference(name)) {
						return true;
					}
					FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
					return factoryBean.isSingleton();
				}
				else {
					return !BeanFactoryUtils.isFactoryDereference(name);
				}
			}
			else {
				return false;
			}
		}
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isPrototype()) {
			// In case of FactoryBean, return singleton status of created object if not a dereference.
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		}
		else {
			// Singleton or scoped - not a prototype.
			// However, FactoryBean may still produce a prototype object...
			if (BeanFactoryUtils.isFactoryDereference(name)) {
				return false;
			}
			if (isFactoryBean(beanName, mbd)) {
				final FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				if (System.getSecurityManager() != null) {
					return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
						@Override
						public Boolean run() {
							return ((factoryBean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factoryBean).isPrototype()) ||
									!factoryBean.isSingleton());
						}
					}, getAccessControlContext());
				}
				else {
					return ((factoryBean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factoryBean).isPrototype()) ||
							!factoryBean.isSingleton());
				}
			}
			else {
				return false;
			}
		}
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		// Check manually registered singletons.
		Object beanInstance = getSingleton(beanName, false);

		LogUtils.info("isTypeMatch beanName :" + beanName +" ,beanInstance name :" +(beanInstance !=null ? beanInstance.getClass().getName() : null));
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean) {
				LogUtils.info("isTypeMatch beanName :" + beanName +" ,beanInstance name :" +(beanInstance !=null ? beanInstance.getClass().getName() : null) + " ,beanInstance   is FactoryBean ");
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					return (type != null && typeToMatch.isAssignableFrom(type));
				}
				else {
					return typeToMatch.isInstance(beanInstance);
				}
			}
			else {
				return (!BeanFactoryUtils.isFactoryDereference(name) && typeToMatch.isInstance(beanInstance));
			}
		}
		else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// null instance registered
			return false;
		}

		else {
			// No singleton instance found -> check bean definition.
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// No bean definition found in this factory -> delegate to parent.
				return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
			}

			// Retrieve corresponding bean definition.
			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			Class<?> classToMatch = typeToMatch.getRawClass();
			Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
					new Class<?>[] {classToMatch} : new Class<?>[] {FactoryBean.class, classToMatch});

			// Check decorated bean definition, if any: We assume it'll be easier
			// to determine the decorated bean's type than the proxy's type.
			BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
			if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
				if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
					return typeToMatch.isAssignableFrom(targetClass);
				}
			}

			Class<?> beanType = predictBeanType(beanName, mbd, typesToMatch);
			if (beanType == null) {
				return false;
			}

			// Check bean class whether we're dealing with a FactoryBean.
			if (FactoryBean.class.isAssignableFrom(beanType)) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					// If it's a FactoryBean, we want to look at what it creates, not the factory class.
					beanType = getTypeForFactoryBean(beanName, mbd);
					if (beanType == null) {
						return false;
					}
				}
			}
			else if (BeanFactoryUtils.isFactoryDereference(name)) {
				// Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
				// type but we nevertheless are being asked to dereference a FactoryBean...
				// Let's check the original bean class and proceed with it if it is a FactoryBean.
				beanType = predictBeanType(beanName, mbd, FactoryBean.class);
				if (beanType == null || !FactoryBean.class.isAssignableFrom(beanType)) {
					return false;
				}
			}

			return typeToMatch.isAssignableFrom(beanType);
		}
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		// Check manually registered singletons.
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
				return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
			}
			else {
				return beanInstance.getClass();
			}
		}
		else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// null instance registered
			return null;
		}

		else {
			// No singleton instance found -> check bean definition.
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// No bean definition found in this factory -> delegate to parent.
				return parentBeanFactory.getType(originalBeanName(name));
			}

			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			// Check decorated bean definition, if any: We assume it'll be easier
			// to determine the decorated bean's type than the proxy's type.
			BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
			if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
				if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
					return targetClass;
				}
			}

			Class<?> beanClass = predictBeanType(beanName, mbd);

			// Check bean class whether we're dealing with a FactoryBean.
			if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					// If it's a FactoryBean, we want to look at what it creates, not at the factory class.
					return getTypeForFactoryBean(beanName, mbd);
				}
				else {
					return beanClass;
				}
			}
			else {
				return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
			}
		}
	}

	@Override
	public String[] getAliases(String name) {
		String beanName = transformedBeanName(name);
		List<String> aliases = new ArrayList<String>();
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		String fullBeanName = beanName;
		if (factoryPrefix) {
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}
		String[] retrievedAliases = super.getAliases(beanName);
		for (String retrievedAlias : retrievedAliases) {
			String alias = (factoryPrefix ? FACTORY_BEAN_PREFIX : "") + retrievedAlias;
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}
		return StringUtils.toStringArray(aliases);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public BeanFactory getParentBeanFactory() {
		return this.parentBeanFactory;
	}

	/**
	 * Does this bean factory contain a bean definition or externally registered singleton
	 * instance with the given name?
	 * <p>If the given name is an alias, it will be translated back to the corresponding
	 * canonical bean name.
	 * <p>If this factory is hierarchical, will ask any parent factory if the bean cannot
	 * be found in this factory instance.
	 * <p>If a bean definition or singleton instance matching the given name is found,
	 * this method will return {@code true} whether the named bean definition is concrete
	 * or abstract, lazy or eager, in scope or not. Therefore, note that a {@code true}
	 * return value from this method does not necessarily indicate that {@link #getBean}
	 * will be able to obtain an instance for the same name.
	 *
	 * bean工厂是否包含一个给定name的bean definition，或者外部被注册的单例bean？
	 * 如果name是一个别名，会被转换成对应的beanName
	 * 如果工厂是有层级的，那么当工厂实例中找不到这个bean时，就会去父工厂中查找
	 * 如果找到匹配name的bean definition或者单例，那么这个方法会返回true
	 * 不管这个bean definition是具体的还是抽象的，提前加载还是懒加载，是否在范围中。
	 * 因此，注意从这个方法中返回的true值并不代表从getBean方法中能够获取一个同名称的实例
	 */
	@Override
	public boolean containsLocalBean(String name) {
		//1.1对name进行必要的转换
		String beanName = transformedBeanName(name);
		LogUtils.info("containsLocalBean beanName : " + beanName ,5);


		boolean containsSingleton = false;
		try {
			containsSingleton = containsSingleton(beanName);
		} catch (Exception e) {
		}
		LogUtils.info("containsLocalBean containsSingleton :" + containsSingleton);
		boolean containsBeanDefinition = false;
		try {
			containsBeanDefinition = containsBeanDefinition(beanName);
		} catch (Exception e) {
		}


		LogUtils.info("containsLocalBean  containsBeanDefinition :" + containsBeanDefinition);
		boolean isFactoryDereference = false;
		try {
			isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name) ;
		} catch (Exception e) {
		}
		LogUtils.info("containsLocalBean   isFactoryDereference :" + isFactoryDereference);
		boolean isFactoryBean = false;
		try {
			isFactoryBean = isFactoryBean(beanName);
		} catch (NoSuchBeanDefinitionException e) {		}
		LogUtils.info("containsLocalBean  isFactoryBean :" +isFactoryBean);


		//singletonObjects或者beanDefinitionMap中已注册beanName则进入条件
		//说明该beanName有对应的bean definition，或者单例bean
		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
				//name开头不为&返回true，如果带了&但是是FactoryBean也返回true
				//要注意下FactoryBean和BeanFactory的区别，可以看下文参考链接
				(!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public void setParentBeanFactory(BeanFactory parentBeanFactory) {
		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
		}
		this.parentBeanFactory = parentBeanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
	}

	@Override
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setTempClassLoader(ClassLoader tempClassLoader) {
		this.tempClassLoader = tempClassLoader;
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return this.tempClassLoader;
	}

	@Override
	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
		this.cacheBeanMetadata = cacheBeanMetadata;
	}

	@Override
	public boolean isCacheBeanMetadata() {
		return this.cacheBeanMetadata;
	}

	@Override
	public void setBeanExpressionResolver(BeanExpressionResolver resolver) {
		this.beanExpressionResolver = resolver;
	}

	@Override
	public BeanExpressionResolver getBeanExpressionResolver() {
		return this.beanExpressionResolver;
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
		this.propertyEditorRegistrars.add(registrar);
	}

	/**
	 * Return the set of PropertyEditorRegistrars.
	 */
	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.isAssignable(PropertyEditor.class, propertyEditorClass);
		this.customEditors.put(requiredType, propertyEditorClass);
	}

	@Override
	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
		registerCustomEditors(registry);
	}

	/**
	 * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
	 */
	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	@Override
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * Return the custom TypeConverter to use, if any.
	 * @return the custom TypeConverter, or {@code null} if none specified
	 */
	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			return customConverter;
		}
		else {
			// Build default TypeConverter, registering custom editors.
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			typeConverter.setConversionService(getConversionService());
			registerCustomEditors(typeConverter);
			return typeConverter;
		}
	}

	@Override
	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.embeddedValueResolvers.add(valueResolver);
	}

	@Override
	public String resolveEmbeddedValue(String value) {
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			if (result == null) {
				return null;
			}
			result = resolver.resolveStringValue(result);
		}
		return result;
	}

	//  可以看到，在 registerBeanPostProcessors 方法实现中其实已经确保了，beanPostProcessor 的唯一性，个人猜想，之所以
	// 选择在 registerBeanPostProcessors 中没有进行重复的移除操作或者是了为保持分类效果，使逻辑更加清楚吧
	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		this.beanPostProcessors.remove(beanPostProcessor);
		this.beanPostProcessors.add(beanPostProcessor);
		if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
			this.hasInstantiationAwareBeanPostProcessors = true;
		}
		if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
			this.hasDestructionAwareBeanPostProcessors = true;
		}
	}

	@Override
	public int getBeanPostProcessorCount() {
		return this.beanPostProcessors.size();
	}

	/**
	 * Return the list of BeanPostProcessors that will get applied
	 * to beans created with this factory.
	 */
	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	/**
	 * Return whether this factory holds a InstantiationAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
	 */
	protected boolean hasInstantiationAwareBeanPostProcessors() {
		return this.hasInstantiationAwareBeanPostProcessors;
	}

	/**
	 * Return whether this factory holds a DestructionAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return this.hasDestructionAwareBeanPostProcessors;
	}

	@Override
	public void registerScope(String scopeName, Scope scope) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		Assert.notNull(scope, "Scope must not be null");
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		Scope previous = this.scopes.put(scopeName, scope);
		if (previous != null && previous != scope) {
			if (logger.isInfoEnabled()) {
				logger.info("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
			}
		}
	}

	@Override
	public String[] getRegisteredScopeNames() {
		return StringUtils.toStringArray(this.scopes.keySet());
	}

	@Override
	public Scope getRegisteredScope(String scopeName) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		return this.scopes.get(scopeName);
	}

	/**
	 * Set the security context provider for this bean factory. If a security manager
	 * is set, interaction with the user code will be executed using the privileged
	 * of the provided security context.
	 */
	public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
		this.securityContextProvider = securityProvider;
	}

	/**
	 * Delegate the creation of the access control context to the
	 * {@link #setSecurityContextProvider SecurityContextProvider}.
	 */
	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.securityContextProvider != null ?
				this.securityContextProvider.getAccessControlContext() :
				AccessController.getContext());
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			this.hasInstantiationAwareBeanPostProcessors = this.hasInstantiationAwareBeanPostProcessors ||
					otherAbstractFactory.hasInstantiationAwareBeanPostProcessors;
			this.hasDestructionAwareBeanPostProcessors = this.hasDestructionAwareBeanPostProcessors ||
					otherAbstractFactory.hasDestructionAwareBeanPostProcessors;
			this.scopes.putAll(otherAbstractFactory.scopes);
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		}
		else {
			setTypeConverter(otherFactory.getTypeConverter());
		}
	}

	/**
	 * Return a 'merged' BeanDefinition for the given bean name,
	 * merging a child bean definition with its parent if necessary.
	 * <p>This {@code getMergedBeanDefinition} considers bean definition
	 * in ancestors as well.
	 * @param name the name of the bean to retrieve the merged definition for
	 * (may be an alias)
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	@Override
	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
		String beanName = transformedBeanName(name);

		// Efficiently check whether bean definition exists in this factory.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
		}
		// Resolve merged bean definition locally.
		return getMergedLocalBeanDefinition(beanName);
	}

	@Override
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			return (beanInstance instanceof FactoryBean);
		}
		else if (containsSingleton(beanName)) {
			// null instance registered
			return false;
		}

		// No singleton instance found -> check bean definition.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}

		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
	}

	@Override
	public boolean isActuallyInCreation(String beanName) {
		return (isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName));
	}

	/**
	 * Return whether the specified prototype bean is currently in creation
	 * (within the current thread).
	 * @param beanName the name of the bean
	 */
	protected boolean isPrototypeCurrentlyInCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		return (curVal != null &&
				(curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
	}

	/**
	 * Callback before prototype creation.
	 * <p>The default implementation register the prototype as currently in creation.
	 * @param beanName the name of the prototype about to be created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal == null) {
			this.prototypesCurrentlyInCreation.set(beanName);
		}
		else if (curVal instanceof String) {
			Set<String> beanNameSet = new HashSet<String>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		}
		else {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * Callback after prototype creation.
	 * <p>The default implementation marks the prototype as not in creation anymore.
	 * @param beanName the name of the prototype that has been created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		}
		else if (curVal instanceof Set) {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.remove(beanName);
			if (beanNameSet.isEmpty()) {
				this.prototypesCurrentlyInCreation.remove();
			}
		}
	}

	@Override
	public void destroyBean(String beanName, Object beanInstance) {
		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to the given bean definition.
	 * @param beanName the name of the bean definition
	 * @param beanInstance the bean instance to destroy
	 * @param mbd the merged bean definition
	 */
	protected void destroyBean(String beanName, Object beanInstance, RootBeanDefinition mbd) {
		new DisposableBeanAdapter(beanInstance, beanName, mbd, getBeanPostProcessors(), getAccessControlContext()).destroy();
	}

	@Override
	public void destroyScopedBean(String beanName) {
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException(
					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
		}
		String scopeName = mbd.getScope();
		Scope scope = this.scopes.get(scopeName);
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope '" + scopeName + "'");
		}
		Object bean = scope.remove(beanName);
		if (bean != null) {
			destroyBean(beanName, bean, mbd);
		}
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * Return the bean name, stripping out the factory dereference prefix if necessary,
	 * and resolving aliases to canonical names.
	 * @param name the user-specified name
	 * @return the transformed bean name
	 *
	 *  返回bean name，剥离factory dereference 前缀，并将别名解析为bean name
	 *
	 */
	protected String transformedBeanName(String name) {
		//总的来说，如果name代表factory，那么name前就带有&前缀，去掉此前缀
		//如果这个name是beanName，则直接返回，如果name是alias，在aliasMap中查找对应的beanName，再返回
		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
	}

	/**
	 * Determine the original bean name, resolving locally defined aliases to canonical names.
	 * @param name the user-specified name
	 * @return the original bean name
	 */
	protected String originalBeanName(String name) {
		String beanName = transformedBeanName(name);
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			beanName = FACTORY_BEAN_PREFIX + beanName;
		}
		return beanName;
	}

	/**
	 * Initialize the given BeanWrapper with the custom editors registered
	 * with this factory. To be called for BeanWrappers that will create
	 * and populate bean instances.
	 * <p>The default implementation delegates to {@link #registerCustomEditors}.
	 * Can be overridden in subclasses.
	 * @param bw the BeanWrapper to initialize
	 * @return
	 * 其中我们看到一个方法是我们熟悉的，就是我们熟悉的，就是 AbstratBeanFactory 类中的 initBeanWrapper 方法，
	 * 这是在 Bean初始化时使用的一个方法，之前已经使用大量的篇幅进行讲解，主要是将 BeanDefinition 转换成 BeanWrapper
	 * 用于对属性的填充，到此，逻辑已经声明了，在 bean 初始化后会调用 ResourceEditorRegistrar 的 registerCustomEditors 方法进行
	 * 批量通过属性编辑器注册，注册后，在属性填充的环节便可以直接让 Spring 使用的这些编辑器进行属性解析了
	 *
	 * 既然提到了 BeanWrapper ，这里也有必要强调下，Spring 中用于封装的 bean 的是 BeanWapper 类型，而它双是间接继承了 PropertyEditorRegistry类型，
	 * 也就是我们之前反复看到的方法参数，PropertyEditorRegistry registry ，其实大部分的情况下都是 BeanWrapper  接口外还继承了 PropertyEditorRegistrySupport
	 * ，在 PropertyEditorRegistrySupport 中有一个这样的方法
	 *
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(getConversionService());
		registerCustomEditors(bw);
	}

	/**
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 * <p>To be called for BeanWrappers that will create and populate bean
	 * instances, and for SimpleTypeConverter used for constructor argument
	 * and factory method type conversion.
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		PropertyEditorRegistrySupport registrySupport =
				(registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
		if (registrySupport != null) {
			registrySupport.useConfigValueEditors();
		}
		if (!this.propertyEditorRegistrars.isEmpty()) {
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					registrar.registerCustomEditors(registry);
				}
				catch (BeanCreationException ex) {
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						BeanCreationException bce = (BeanCreationException) rootCause;
						if (isCurrentlyInCreation(bce.getBeanName())) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
										"] failed because it tried to obtain currently created bean '" +
										ex.getBeanName() + "': " + ex.getMessage());
							}
							onSuppressedException(ex);
							continue;
						}
					}
					throw ex;
				}
			}
		}
		if (!this.customEditors.isEmpty()) {
			for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
				Class<?> requiredType = entry.getKey();
				Class<? extends PropertyEditor> editorClass = entry.getValue();
				registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass));
			}
		}
	}


	/**
	 * Return a merged RootBeanDefinition, traversing the parent bean definition
	 * if the specified bean corresponds to a child bean definition.
	 * @param beanName the name of the bean to retrieve the merged definition for
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// Quick check on the concurrent map first, with minimal locking.
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null) {
			LogUtils.info("getMergedLocalBeanDefinition mergedBeanDefinitions has Bean " + beanName);
			return mbd;
		}
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}

	/**
	 * Return a RootBeanDefinition for the given top-level bean, by merging with
	 * the parent if the given bean's definition is a child bean definition.
	 * @param beanName the name of the bean definition
	 * @param bd the original bean definition (Root/ChildBeanDefinition)
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
			throws BeanDefinitionStoreException {

		return getMergedBeanDefinition(beanName, bd, null);
	}

	/**
	 * Return a RootBeanDefinition for the given bean, by merging with the
	 * parent if the given bean's definition is a child bean definition.
	 * @param beanName the name of the bean definition
	 * @param bd the original bean definition (Root/ChildBeanDefinition)
	 * @param containingBd the containing bean definition in case of inner bean,
	 * or {@code null} in case of a top-level bean
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedBeanDefinition(
			String beanName, BeanDefinition bd, BeanDefinition containingBd)
			throws BeanDefinitionStoreException {
		LogUtils.info("getMergedBeanDefinition beanName " + beanName + " , bd Name = " + bd.getClass().getName() + "  containingBd Name " +
				(containingBd !=null ? containingBd.getClass().getName() : null),5 );

		for(Map.Entry<String, RootBeanDefinition> map: this.mergedBeanDefinitions.entrySet()){
			LogUtils.info("getMergedBeanDefinition key " + map.getKey() + " , value = " + map.getValue());
		}
		synchronized (this.mergedBeanDefinitions) {
			RootBeanDefinition mbd = null;

			// Check with full lock now in order to enforce the same merged instance.
			if (containingBd == null) {
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			if (mbd == null) {
				LogUtils.info("getMergedBeanDefinition mbd  is null");

				if (bd.getParentName() == null) {
					LogUtils.info("getMergedBeanDefinition parentName  is null beanName :" + bd.getClass().getName());
					// Use copy of given root bean definition.
					if (bd instanceof RootBeanDefinition) {
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
						LogUtils.info("getMergedBeanDefinition bd instanceof RootBeanDefinition  beanName :" + mbd.getClass().getName());
					}
					else {

						mbd = new RootBeanDefinition(bd);
						LogUtils.info("getMergedBeanDefinition mbd beanName :" + mbd.getClass().getName());
					}
				}
				else {
					// Child bean definition: needs to be merged with parent.
					BeanDefinition pbd;
					try {
						String parentBeanName = transformedBeanName(bd.getParentName());
						if (!beanName.equals(parentBeanName)) {
							pbd = getMergedBeanDefinition(parentBeanName);
						}
						else {
							if (getParentBeanFactory() instanceof ConfigurableBeanFactory) {
								pbd = ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(parentBeanName);
							}
							else {
								throw new NoSuchBeanDefinitionException(bd.getParentName(),
										"Parent name '" + bd.getParentName() + "' is equal to bean name '" + beanName +
										"': cannot be resolved without an AbstractBeanFactory parent");
							}
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}
					// Deep copy with overridden values.
					mbd = new RootBeanDefinition(pbd);
					mbd.overrideFrom(bd);
				}

				// Set default singleton scope, if not configured before.
				if (!StringUtils.hasLength(mbd.getScope())) {
					LogUtils.info("getMergedBeanDefinition setScope  singleton  " );
					mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
				}

				// A bean contained in a non-singleton bean cannot be a singleton itself.
				// Let's correct this on the fly here, since this might be the result of
				// parent-child merging for the outer bean, in which case the original inner bean
				// definition will not have inherited the merged outer bean's singleton status.
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					LogUtils.info("getMergedBeanDefinition containingBd  !=null  && containingBd is not single && mbd is singleon  " );
					mbd.setScope(containingBd.getScope());
				}

				// Only cache the merged bean definition if we're already about to create an
				// instance of the bean, or at least have already created an instance before.
				if (containingBd == null && isCacheBeanMetadata()) {
					LogUtils.info("getMergedBeanDefinition containingBd == null && isCacheBeanMetadata " );
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}


			return mbd;
		}
	}

	/**
	 * Check the given merged bean definition,
	 * potentially throwing validation exceptions.
	 * @param mbd the merged bean definition to check
	 * @param beanName the name of the bean
	 * @param args the arguments for bean creation, if any
	 * @throws BeanDefinitionStoreException in case of validation failure
	 */
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, Object[] args)
			throws BeanDefinitionStoreException {

		if (mbd.isAbstract()) {
			throw new BeanIsAbstractException(beanName);
		}
	}

	/**
	 * Remove the merged bean definition for the specified bean,
	 * recreating it on next access.
	 * @param beanName the bean name to clear the merged definition for
	 */
	protected void clearMergedBeanDefinition(String beanName) {
		this.mergedBeanDefinitions.remove(beanName);
	}

	/**
	 * Clear the merged bean definition cache, removing entries for beans
	 * which are not considered eligible for full metadata caching yet.
	 * <p>Typically triggered after changes to the original bean definitions,
	 * e.g. after applying a {@code BeanFactoryPostProcessor}. Note that metadata
	 * for beans which have already been created at this point will be kept around.
	 * @since 4.2
	 */
	public void clearMetadataCache() {
		Iterator<String> mergedBeans = this.mergedBeanDefinitions.keySet().iterator();
		while (mergedBeans.hasNext()) {
			if (!isBeanEligibleForMetadataCaching(mergedBeans.next())) {
				mergedBeans.remove();
			}
		}
	}

	/**
	 * Resolve the bean class for the specified bean definition,
	 * resolving a bean class name into a Class reference (if necessary)
	 * and storing the resolved Class in the bean definition for further use.
	 * @param mbd the merged bean definition to determine the class for
	 * @param beanName the name of the bean (for error handling purposes)
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the resolved bean class (or {@code null} if none)
	 * @throws CannotLoadBeanClassException if we failed to load the class
	 */
	protected Class<?> resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch)
			throws CannotLoadBeanClassException {
		try {
			if (mbd.hasBeanClass()) {
				return mbd.getBeanClass();
			}
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
					@Override
					public Class<?> run() throws Exception {
						return doResolveBeanClass(mbd, typesToMatch);
					}
				}, getAccessControlContext());
			}
			else {
				return doResolveBeanClass(mbd, typesToMatch);
			}
		}
		catch (PrivilegedActionException pae) {
			ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		}
		catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		}
		catch (LinkageError err) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
		}
	}

	private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch) throws ClassNotFoundException {
		ClassLoader beanClassLoader = getBeanClassLoader();
		ClassLoader classLoaderToUse = beanClassLoader;
		if (!ObjectUtils.isEmpty(typesToMatch)) {
			// When just doing type checks (i.e. not creating an actual instance yet),
			// use the specified temporary class loader (e.g. in a weaving scenario).
			ClassLoader tempClassLoader = getTempClassLoader();
			if (tempClassLoader != null) {
				classLoaderToUse = tempClassLoader;
				if (tempClassLoader instanceof DecoratingClassLoader) {
					DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
					for (Class<?> typeToMatch : typesToMatch) {
						dcl.excludeClass(typeToMatch.getName());
					}
				}
			}
		}
		String className = mbd.getBeanClassName();
		if (className != null) {
			Object evaluated = evaluateBeanDefinitionString(className, mbd);
			if (!className.equals(evaluated)) {
				// A dynamically resolved expression, supported as of 4.2...
				if (evaluated instanceof Class) {
					return (Class<?>) evaluated;
				}
				else if (evaluated instanceof String) {
					return ClassUtils.forName((String) evaluated, classLoaderToUse);
				}
				else {
					throw new IllegalStateException("Invalid class name expression result: " + evaluated);
				}
			}
			// When resolving against a temporary class loader, exit early in order
			// to avoid storing the resolved Class in the bean definition.
			if (classLoaderToUse != beanClassLoader) {
				return ClassUtils.forName(className, classLoaderToUse);
			}
		}
		return mbd.resolveBeanClass(beanClassLoader);
	}

	/**
	 * Evaluate the given String as contained in a bean definition,
	 * potentially resolving it as an expression.
	 * @param value the value to check
	 * @param beanDefinition the bean definition that the value comes from
	 * @return the resolved value
	 * @see #setBeanExpressionResolver
	 * 之前我们讲解过 Spring 在 bean 进行初始化的时候会有一个属性填充一步，而这一步中，Spring  会调用 AbstractAutowireCapableBeanFactory
	 * 类的 applyPropertValues  函数来完成功能，就是这个函数中，会通过构造 BeanDefinitionValueResolver 类型实例 valueResolver 来进行属性值
	 * 的解析，同时，也是在这一步骤中一般通过 AbstractBeanFactory 中的 evaluateBeanDefinitionString 方法来完成 SPEL 解析
	 * 当调用这个方法时会判断是否存在语言解析器，如果存在则调用语言解析器的方法进行解析，解析的过程就是在 Spring 的 expression 的包内
	 * 这里不做过多的解析，我们通过查看 evaluateBeanDefinitionString 方法调用层次可以看出，应用语言解析器的调用主要是在解析依赖
	 * 注入 bean 的时候，以及完成 bean 的初始化和属性获取后进行属性填充的时候
	 *
	 */
	protected Object evaluateBeanDefinitionString(String value, BeanDefinition beanDefinition) {
		if (this.beanExpressionResolver == null) {
			return value;
		}
		Scope scope = (beanDefinition != null ? getRegisteredScope(beanDefinition.getScope()) : null);
		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
	}


	/**
	 * Predict the eventual bean type (of the processed bean instance) for the
	 * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
	 * Does not need to handle FactoryBeans specifically, since it is only
	 * supposed to operate on the raw bean type.
	 * <p>This implementation is simplistic in that it is not able to
	 * handle factory methods and InstantiationAwareBeanPostProcessors.
	 * It only predicts the bean type correctly for a standard bean.
	 * To be overridden in subclasses, applying more sophisticated type detection.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition to determine the type for
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type of the bean, or {@code null} if not predictable
	 */
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		if (mbd.getFactoryMethodName() != null) {
			return null;
		}
		return resolveBeanClass(mbd, beanName, typesToMatch);
	}

	/**
	 * Check whether the given bean is defined as a {@link FactoryBean}.
	 * @param beanName the name of the bean
	 * @param mbd the corresponding bean definition
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
		return (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
	}

	/**
	 * Determine the bean type for the given FactoryBean definition, as far as possible.
	 * Only called if there is no singleton instance registered for the target bean already.
	 * <p>The default implementation creates the FactoryBean via {@code getBean}
	 * to call its {@code getObjectType} method. Subclasses are encouraged to optimize
	 * this, typically by just instantiating the FactoryBean but not populating it yet,
	 * trying whether its {@code getObjectType} method already returns a type.
	 * If no type found, a full FactoryBean creation as performed by this implementation
	 * should be used as fallback.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @return the type for the bean if determinable, or {@code null} else
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 */
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		if (!mbd.isSingleton()) {
			return null;
		}
		try {
			FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
			return getTypeForFactoryBean(factoryBean);
		}
		catch (BeanCreationException ex) {
			if (ex instanceof BeanCurrentlyInCreationException) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean currently in creation on FactoryBean type check: " + ex);
				}
			}
			else {
				if (logger.isWarnEnabled()) {
					logger.warn("Bean creation exception on FactoryBean type check: " + ex);
				}
			}
			onSuppressedException(ex);
			return null;
		}
	}

	/**
	 * Mark the specified bean as already created (or about to be created).
	 * <p>This allows the bean factory to optimize its caching for repeated
	 * creation of the specified bean.
	 * @param beanName the name of the bean
	 */
	protected void markBeanAsCreated(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			this.alreadyCreated.add(beanName);

			// Let the bean definition get re-merged now that we're actually creating
			// the bean... just in case some of its metadata changed in the meantime.
			clearMergedBeanDefinition(beanName);
		}
	}

	/**
	 * Perform appropriate cleanup of cached metadata after bean creation failed.
	 * @param beanName the name of the bean
	 */
	protected void cleanupAfterBeanCreationFailure(String beanName) {
		this.alreadyCreated.remove(beanName);
	}

	/**
	 * Determine whether the specified bean is eligible for having
	 * its bean definition metadata cached.
	 * @param beanName the name of the bean
	 * @return {@code true} if the bean's metadata may be cached
	 * at this point already
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.contains(beanName);
	}

	/**
	 * Remove the singleton instance (if any) for the given bean name,
	 * but only if it hasn't been used for other purposes than type checking.
	 * @param beanName the name of the bean
	 * @return {@code true} if actually removed, {@code false} otherwise
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			removeSingleton(beanName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Get the object for the given bean instance, either the bean
	 * instance itself or its created object in case of a FactoryBean.
	 * @param beanInstance the shared bean instance
	 * @param name name that may include factory dereference prefix
	 * @param beanName the canonical bean name
	 * @param mbd the merged bean definition
	 * @return the object to expose for the bean
	 * 获取给定的Bean的实例对象，主要完成FactoryBean 的相关处理
	 *
	 * 从这个代码中来看，其实这个方法并没有什么重要的信息，大多都是辅助的代码以及一些功能性的判断，而真正的核心代码去委托给了 getObjectFromFactoryBean
	 * ,我们来看看 getObjectFromBeanInstance 中所做的工作
	 *
	 * 	 1. 对 FactoryBean 正确性的验证
	 * 	 2.对非 FactoryBean 不做任何处理
	 * 	 3.对 bean 进行转换
	 * 	 4.将从 Factory 上解析 bean 的工作委托给 getObjectFromFactoryBean
	 *
	 */
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		// 容器已经得到了Bean实例对象，这个实例对象可能是一个普通的Bean
		// 也可能是一个工厂Bean,如果是一个工厂Bean,则使用它创建一个Bean的实例对象
		// 如果调用本身也想获得一个容器的引用，则返回这个工厂Bean实例对象
		// 如果指定的名称是容器的解引用(dereference，即对象本身而非内存地址)
		// 且Bean实例也不是创建Bean实例对象的工厂Bean |
		// 如果指定的name是工厂相关的以(&为前缀)且 beanInstance 又不是 FactoryBean 类型则验证不通过
		if (BeanFactoryUtils.isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
		}

		LogUtils.info("getObjectForBeanInstance mbd Name :" + (mbd !=null ? mbd.getClass().getName() : null));

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.
		// 如果Bean 实例不是工厂Bean,或者指定名称是容器的解引用
		// 调用者获取对容器的引用时，直接返回当前的Bean的实例 | 现在我们有了个bean 的实例，这个实例可能会是正常的 bean 或者是 FactoryBean
		//  如果是 FactoryBean 我们使用它创建实例，但是如果用户想要直接获取工厂实例而不是工厂的 getObject 方法对应的实例，那么传入的 name 应该加入前缀 &
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			LogUtils.info("getObjectForBeanInstance xxxxxxxx  beanInstance name :" + beanInstance.getClass().getName());
			return beanInstance;
		}


		// 处理指定名称不是容器的解引用，或者根据名称获取Bean 实例对象是一个工厂Bean
		// 使用工厂Bean创建一个Bean 的实例对象
		// 加载 FactoryBean
		Object object = null;
		if (mbd == null) {
			// 从Bean工厂缓存中获取指定名称的Bean的实例对象| 尝试从缓存中加载 bean
			object = getCachedObjectForFactoryBean(beanName);
		}
		// 让Bean 工厂产生指定名称Bean的实例对象
		if (object == null) {
			// Return bean instance from factory.
			// 到这里已经里已经明确的知道 beanInstance 一定是一个 FactoryBean 类型
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// 如果从Bean 工厂生成的Bean 是单例模式，则缓存
			// Caches object obtained from FactoryBean if it is a singleton.
			// containsBeanDefinition检测 beanDefinitionMap 中也就是所有的已经加载的类中检测是否定义了 beanName
			if (mbd == null && containsBeanDefinition(beanName)) {
				// 从容器中获取指定名称的Bean的定义，如果继承了基类，则合并基类的相关的属性 |  将存储在 XML 配置文件中的 GernericBeanDefinition
				// 转换成 RootBeanDefinition，如果指定的是 beanName 是子的 bean 的话，同时会合并父类的相关的属性
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			// 如果从容器中得到了Bean定义信息，并且Bean定义信息不是虚构的
			// 则让工厂Bean产生Bean的实例对象
			// 是否用户定义的而不是应用程序本身定义的
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			// 调用FactoryBeanRegistrySupport类的getObjectFromFactoryBean()方法
			// 实现工厂Bean生产Bean实例对象的过程
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}

	/**
	 * Determine whether the given bean name is already in use within this factory,
	 * i.e. whether there is a local bean or alias registered under this name or
	 * an inner bean created with this name.
	 * @param beanName the name to check
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * Determine whether the given bean requires destruction on shutdown.
	 * <p>The default implementation checks the DisposableBean interface as well as
	 * a specified destroy method and registered DestructionAwareBeanPostProcessors.
	 * @param bean the bean instance to check
	 * @param mbd the corresponding bean definition
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see AbstractBeanDefinition#getDestroyMethodName()
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
		return (bean != null &&
				(DisposableBeanAdapter.hasDestroyMethod(bean, mbd) || hasDestructionAwareBeanPostProcessors()));
	}

	/**
	 * Add the given bean to the list of disposable beans in this factory,
	 * registering its DisposableBean interface and/or the given destroy method
	 * to be called on factory shutdown (if applicable). Only applies to singletons.
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 * @param mbd the bean definition for the bean
	 * @see RootBeanDefinition#isSingleton
	 * @see RootBeanDefinition#getDependsOn
	 * @see #registerDisposableBean
	 * @see #registerDependentBean
	 * Spring 中不但提供了对初始化方法入口的扩展，同样也提供了销毁方法的扩展入口，对于销毁方法的扩展，除了我们熟知的配置属性
	 *  destroy-method 方法外，用户还没写作业可以注册后处理 DestructionAwareBeanPostProcessor 来统一处理 bean 的销毁方法
	 *  代码如下：
	 *
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
				// Register a DisposableBean implementation that performs all destruction
				// work for the given bean: DestructionAwareBeanPostProcessors,
				// DisposableBean interface, custom destroy method.
				// 单例模式注册需要销毁的 bean ,此方法中会处理实现 DisposableBean 的 bean ,并且对所有的 bean 使用
				// DestructionAwareBeanPostProcessors的处理，
				// DisposableBean DestructionAwareBeanPostProcessors

				registerDisposableBean(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
			else {
				// A bean with a custom scope...
				// 自定义 scope 的处理
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
		}
	}


	//---------------------------------------------------------------------
	// Abstract methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * Does not consider any hierarchy this factory may participate in.
	 * Invoked by {@code containsBean} when no cached singleton instance is found.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
	 * @see #containsBean
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 */
	protected abstract boolean containsBeanDefinition(String beanName);

	/**
	 * Return the bean definition for the given bean name.
	 * Subclasses should normally implement caching, as this method is invoked
	 * by this class every time bean definition metadata is needed.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 * @param beanName the name of the bean to find a definition for
	 * @return the BeanDefinition for this prototype name (never {@code null})
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException
	 * if the bean definition cannot be resolved
	 * @throws BeansException in case of errors
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * Create a bean instance for the given merged bean definition (and arguments).
	 * The bean definition will already have been merged with the parent definition
	 * in case of a child definition.
	 * <p>All bean retrieval methods delegate to this method for actual bean creation.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param args explicit arguments to use for constructor or factory method invocation
	 * @return a new instance of the bean
	 * @throws BeanCreationException if the bean could not be created
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, Object[] args)
			throws BeanCreationException;

}
