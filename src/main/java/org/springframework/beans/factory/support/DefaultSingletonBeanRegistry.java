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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.test.LogUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * Internal marker for a null singleton object:
	 * used as marker value for concurrent Maps (which don't support null values).
	 */
	protected static final Object NULL_OBJECT = new Object();


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Cache of singleton objects: bean name --> bean instance */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(64);

	/** Cache of singleton factories: bean name --> ObjectFactory */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

	/** Cache of early singleton objects: bean name --> bean instance */
	private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

	/** Set of registered singletons, containing the bean names in registration order */
	private final Set<String> registeredSingletons = new LinkedHashSet<String>(64);

	/** Names of beans that are currently in creation */
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

	/** Names of beans currently excluded from in creation checks */
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

	/** List of suppressed Exceptions, available for associating related causes */
	private Set<Exception> suppressedExceptions;

	/** Flag that indicates whether we're currently within destroySingletons */
	private boolean singletonsCurrentlyInDestruction = false;

	/** Disposable bean instances: bean name --> disposable instance */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();

	/** Map between containing bean names: bean name --> Set of bean names that the bean contains */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>(16);

	/** Map between dependent bean names: bean name --> Set of dependent bean names */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

	/** Map between depending bean names: bean name --> Set of bean names for the bean's dependencies */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

	// 2.1调用父类方法，注册单例

	/**
	 *  在给定的bean name下，将存在的对象作为单例注册在工厂中
	 *  给定的实例应该是完全初始化；工厂不执行任何初始化回调（特别是，他不会调用InitializingBean的
	 *  afterPropertiesSet方法）
	 *  给定的实例也不接收任何销毁回调（像DisposableBean的destroy方法）
	 *  当在完整的BeanFactory运行时：
	 *  如果你的bean需要接收初始化或者销毁的回调，注册一个bean definition替代一个存在的实例
	 *  通常此方法在工厂配置时被调用，也能在运行时单例注册时被调用。
	 *  作为结果，工厂的实现应该同步单例的访问；如果支持BeanFactory的单例的延迟初始化就不得不这样做
	 */
	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		LogUtils.info("registerSingleton " ,5);
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			Object oldObject = this.singletonObjects.get(beanName);
			//不能注册两次
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			//进入这个方法
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			//singletonObjects是一个ConcurrentHashMap
			//用来缓存单例对象
			this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
			//singletonFactories是一个HashMap
			//里面缓存着单例工厂
			this.singletonFactories.remove(beanName);
			//早期单例对象
			//earlySingletonObjects是一个HashMap
			this.earlySingletonObjects.remove(beanName);
			//registeredSingletons是一个LinkedHashSet
			//被注册单例的集合，以注册的顺序包含着bean name
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	public Object getSingleton(String beanName) {
		// 参数 true 设置标识允许早期依赖
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 * 介绍过 FactoryBean 的用法后，我们就可以了解 bean 的加载过程了，前面已经提到过，单例在 Spring 的同一个容器内只会被创建一次
	 * ，后续再获取 bean 直接从单例缓存中获取，当然这里中介尝试加载，首先尝试从缓存中加载，然后再次尝试从 singletonFactories 中加载
	 * 因为在创建单例 bean 的时候会存在依赖注入的情况，而在创建依赖的时候为了避免循环依赖，Spring 在创建 bean 的原则是不等 bean获取
	 * 创建完成就会将创建bean的 ObjectFactory 提早曝光加入到缓存中，一旦下一个 bean 创建的时候需要依赖上一个 bean，则直接使用 ObjectFactory
	 *
	 * 这个方法因为涉及循环依赖的检测，以及涉及很多的变量的记录存取，所以让很多的读者摸不着头脑，这个方法首先尝试从 singletonObjects 里面
	 * 获取实例，如果获取不到，再从 earlySingletonObjects 里来获取，如果还是获取歪，再尝试从 singletonFactories 里面获取BeanName 对应的
	 * 对应的 ObjectFactory ，然后调用这个 ObjectFacotry 里的 getObject 来创建 bean ,并放到，并放到 earlySingletonObject 里面去
	 * 并且从 singletonFactories 里面 remove掉这个 ObjectFactory ，而对后续的所有的内存操作都只为了循环依赖检测的时候用，也就是在
	 * allowEarlyReference 为true 的情况下才会使用
	 *
	 * 这里可能会涉及到不同存储 bean 的不同的 map ,可能让读者感到崩溃，简单的解释如下
	 * singletonObjects : 用于保存 BeanName 和创建 bean实例之间的关系，bean name -> bean instance
	 * singletonFactories : 用于保存 beanName 和创建 bean 工厂之间的关系，bean name --> object Factory
	 * earlySingletonObjects : 也是保存 BeanName 和创建 bean 实例之间的关系，与 singleonObjects 的不同之处在于，当一个单例 bean
	 *  被放置在里面的时候，那么当 bean 还在创建过程中，就可以通过 getBean 方法获取到了，其上的是用来检测循环引用，
	 *  registerdSingletons :  用来保存当前已经注册的 bean
	 *
	 * 在 getBean 方法中，getObjectForBeanInstance 是个高频率使用的方法，无论是从缓存中获得bean 还是根据不同的 scope 策略来加载 bean
	 * 还是根据不同的 scope 策略加载 bean ，总之，我们得到 bean 的实例后要做的第一步就是调用这个方法来检测一下正确性，其实就是用于检测当前 bean
	 * 是否 FactoryBean 类型的 bean ,如果是，那么需要调用该 bean 对应的 factoryBean 实例中的 getObject()作为返回值
	 *
	 * 无论是从缓存中获取到 bean 还是通过不同的 scope 策略来加载 bean 都只是最原始的 bean 的状态，并不一定是我们最终想要 的 bean ,举个例子
	 * 假如我们需要工厂 bean 进行处理，那么这里得到的其实是工厂 bean 的初始状态，但是我们真正的需要的工厂 bean 中的定义的 factory-method
	 * 方法中返回的 bean ,而 getObjectForBeanInstance 方法就是这时完成工作的
	 *
	 *
	 *
	 *
	 *
	 */
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// 检查缓存中是否存在实例
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			//如果为空，锁定全局变量并进行处理
			synchronized (this.singletonObjects) {
				//  如果此 bean 正在加载，则不进行处理
				singletonObject = this.earlySingletonObjects.get(beanName);
				if (singletonObject == null && allowEarlyReference) {
					// 当某些方法需要提前初始化的时候则会调用 addSingletonFactory 方法将对应的 ObjectFactory 初始化策略存储在 singleFactories 中
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						// 调用预先设定的 getObject 方法
						singletonObject = singletonFactory.getObject();
						// 记录在缓存中的,earlySingletonObjects 和 singleFactories 互斥
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return (singletonObject != NULL_OBJECT ? singletonObject : null);
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 * 之前我们已经讲解了从缓存中获取单例的过程，那么，如果缓存中不存在已经加载的单例 bean ，就需要从头开始 bean 的加载过程了，而 Spring
	 * 中使用 getSingleton 的重载方法实现了 bean 的加载过程
	 * 上述代码中其实使用了回调方法，使得程序可以在单例创建前后做一些准备及处理操作，而真正的获取单例 bean 的方法其实并不是在此方法中实现在
	 * 其实逻辑是在 ObjectFactory 类型的实例 singleFactory 中实现的，而这些准备及处理操作包括如下内容：
	 *  1.检查缓存中是否已经加载过
	 *  2.若没有加载，则记录 beanName 的正在加载状态
	 *  3.加载单例前记录加载状态
	 *
	 *  可能你会觉得 beforeSingletonCreation 方法是空实现，里面没有任何逻辑，但是其实不是，这个函数中做了一个很重要的操作，记录加载状态
	 *  ，也就是通过 this.singletonsCurrentlyIn 这个函数做一个很重要的操作，记录加载状态，也就是通过 this.singletonsCurrentlyInCreation.add(beanName )
	 *  将当前正要创建的 bean 记录在缓存中，这样便可以对我一依赖进行检测
	 *
	 *
	 * protected void beforeSingletonCreation(String beanName ){
	 *     if(!this.inCreationCheckExclusions.contains(beanName ) && !this.singletons.CurrentlyInCreation.add(beanName )){
	 *         throw new BeanCurrentlyInCreationException(beanName);
	 *     }
	 * }
	 * 4.通过调用参数传入的 ObjectFacotry 的个体 Object 方法实例化 bean
	 * 5.加载单例后的处理方法调用
	 * 同步骤3的记录加载状态相似，当 bean 加载结束后需要移除缓存中对该 bean 的正在加载状态记录
	 * protected void afterSingletonCreation(String beanName ){
	 *     if(!this.inCreationCheckExclusions.contains(beanName) && !this.singletons.CurrentlyInCreation.remove(beanName)){
	 *         throw new IllegalStateException("Single '" + beanName + " ' isnt current in creation ")
	 *     }
	 * }
	 * 6.将结果记录至缓存中并删除加载bean 的过程中所记录的各种辅助状态
	 * protected void addSingleton(String beanName ,Object singletonObject){
	 *     synchronized(this.singletonObjets){
	 *         this.singletonObjects.put(beanName,(singletonObject == null ? singletonObject:NULL_OBJECT));
	 *         this.singletonFactories.remove(beanName);
	 *         this.earlySingletonObjects.remove(beanName);
	 *         this.registereSingletons.add(beanName);
	 *     }
	 * }
	 * 7.返回处理结果
	 * 虽然我们已经从外部了解了加载 bean 的逻辑架构，但现在我们还并没有开始对 bean 的加载功能的探索，之前反映到过，bean 的加载逻辑其实
	 * 是在传入的 ObjectFactory 类型参数 singletonFactory 中定义的，我们反推参数的获取 ，得到如下的代码
	 * sharedInstance=getSingleton(beanName ,new ObjectFactory<Object> (){
	 *    public Object getObject() throws BeanException{
	 *        try{
	 *            return createBean(beanName ,mbd,args);
	 *        }catch(Exception ex ){
	 *            destroySingleton(beanName);
	 *            throw ex;
	 *        }
	 *    }
	 * });
	 * ObjectFactory 的核心的部分其实只是调用了 createBean的方法，所以我们还需要到 createBean 方法中追寻真理
	 *
	 *
	 *
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "'beanName' must not be null");
		// 全局变量需要同步
		synchronized (this.singletonObjects) {
			// 首先检查对应的bean 是否已经加载过，因为 singleton 模式其实就是复用以创建 bean ，这一步是必须的
			Object singletonObject = this.singletonObjects.get(beanName);
			// 如果为空才可以进行 singletonObject == null
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while the singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<Exception>();
				}
				try {
					// 初始化 bean
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}

				if (newSingleton) {
					// 加入缓存中
					addSingleton(beanName, singletonObject);
				}
			}
			return (singletonObject != NULL_OBJECT ? singletonObject : null);
		}
	}

	/**
	 * Register an Exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * @param ex the Exception to register
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		// A quick check for an existing entry upfront, avoiding synchronization...
		Set<String> containedBeans = this.containedBeanMap.get(containingBeanName);
		if (containedBeans != null && containedBeans.contains(containedBeanName)) {
			return;
		}

		// No entry yet -> fully synchronized manipulation of the containedBeans Set
		synchronized (this.containedBeanMap) {
			containedBeans = this.containedBeanMap.get(containingBeanName);
			if (containedBeans == null) {
				containedBeans = new LinkedHashSet<String>(8);
				this.containedBeanMap.put(containingBeanName, containedBeans);
			}
			containedBeans.add(containedBeanName);
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 *                          为指定的Bean注入依赖的Bean
	 *
	 *                          1.对Bean的属性调用getBean()方法 ，完成依赖Bean的初始化和依赖注入
	 *                          2.将依赖Bean的属性引用设置到被依赖的Bean的属性上
	 *                          3.将依赖的Bean的名称和被依赖的Bean的名称存储到IoC容器的集合中
	 *
	 *                          Spring IoC 容器的autowiring自动属性依赖注入是一个很方便的特性，可以简化开发配置，但是凡事都有两面
	 *                          性，自动属性依赖注入也有不足，首先，Bean的依赖关系在配置文件中无法很清楚的看出来，会给维护造成一定的
	 *                          困难，其实，由于自动属性依赖注入是Spring 容器自动执行的，容器是会智能的判断的，如果配置不当用，将会
	 *                         带来无法预料的后果，所以在使用自动属性依赖注入的时候需要综合的考虑
	 *
	 *
	 *
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		// A quick check for an existing entry upfront, avoiding synchronization...
		// 处理Bean的名称，将别名转换成规范的Bean的名称
		String canonicalName = canonicalName(beanName);
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans != null && dependentBeans.contains(dependentBeanName)) {
			return;
		}

		// No entry yet -> fully synchronized manipulation of the dependentBeans Set
		// 多线程同步，保证容器内数据是一致的
		// 在容器中通过Bean名称->全部依赖Bean的名称的集合，查找 指定的名称的Bean的依赖Bean
		synchronized (this.dependentBeanMap) {
			// 获取指定名称Bean，所有的依赖Bean的名称
			dependentBeans = this.dependentBeanMap.get(canonicalName);
			if (dependentBeans == null) {
				// 为Bean 设置依赖Bean信息
				dependentBeans = new LinkedHashSet<String>(8);
				this.dependentBeanMap.put(canonicalName, dependentBeans);
			}
			// 在向容器中通过Bean 名称全部依赖Bean的名称集合，添加Bean的依赖信息
			// 即，将Bean所依赖的Bean添加到容器集合中
			dependentBeans.add(dependentBeanName);
		}
		//在向容器中通过Bean 名称-> 指定名称Bean依赖Bean集合，查找指定名称的Bean的依赖Bean
		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
			if (dependenciesForBean == null) {
				dependenciesForBean = new LinkedHashSet<String>(8);
				this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
			}
			// 在容器中通过Bean 名称指定Bean的依赖Bean名称集合，添加Bean的依赖信息
			// 即将Bean所有的依赖的Bean添加到容器的集合中
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		return isDependent(beanName, dependentBeanName, null);
	}

	private boolean isDependent(String beanName, String dependentBeanName, Set<String> alreadySeen) {
		String canonicalName = canonicalName(beanName);
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<String>();
			}
			alreadySeen.add(beanName);
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		return StringUtils.toStringArray(dependentBeans);
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		return dependenciesForBean.toArray(new String[dependenciesForBean.size()]);
	}

	public void destroySingletons() {
		if (logger.isDebugEnabled()) {
			logger.debug("Destroying singletons in " + this);
		}
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		Set<String> dependencies = this.dependentBeanMap.remove(beanName);
		if (dependencies != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				logger.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
			}
		}

		// Trigger destruction of contained beans...
		Set<String> containedBeans = this.containedBeanMap.remove(beanName);
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
