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

package org.springframework.context.weaving;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor}
 * implementation that passes the context's default {@link LoadTimeWeaver}
 * to beans that implement the {@link LoadTimeWeaverAware} interface.
 *
 * <p>{@link org.springframework.context.ApplicationContext Application contexts}
 * will automatically register this with their underlying {@link BeanFactory bean factory},
 * provided that a default {@code LoadTimeWeaver} is actually available.
 *
 * <p>Applications should not use this class directly.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see LoadTimeWeaverAware
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public class LoadTimeWeaverAwareProcessor implements BeanPostProcessor, BeanFactoryAware {

	private LoadTimeWeaver loadTimeWeaver;

	private BeanFactory beanFactory;


	/**
	 * Create a new {@code LoadTimeWeaverAwareProcessor} that will
	 * auto-retrieve the {@link LoadTimeWeaver} from the containing
	 * {@link BeanFactory}, expecting a bean named
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
	 */
	public LoadTimeWeaverAwareProcessor() {
	}

	/**
	 * Create a new {@code LoadTimeWeaverAwareProcessor} for the given
	 * {@link LoadTimeWeaver}.
	 * <p>If the given {@code loadTimeWeaver} is {@code null}, then a
	 * {@code LoadTimeWeaver} will be auto-retrieved from the containing
	 * {@link BeanFactory}, expecting a bean named
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
	 * @param loadTimeWeaver the specific {@code LoadTimeWeaver} that is to be used
	 */
	public LoadTimeWeaverAwareProcessor(LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	/**
	 * Create a new {@code LoadTimeWeaverAwareProcessor}.
	 * <p>The {@code LoadTimeWeaver} will be auto-retrieved from
	 * the given {@link BeanFactory}, expecting a bean named
	 * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
	 * @param beanFactory the BeanFactory to retrieve the LoadTimeWeaver from
	 */
	public LoadTimeWeaverAwareProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/***
	 * LoadTimeWeaverAwareProcessor 实现 BeanPostProcessor 方法，那么对于 BeanPostProcessor 接口来讲，postProcessBeforeInitialization
	 * 与 postProcessAfterInitialization 有着特殊的意义，也就是说在所有的 bean 初始化之前与之后都会分别调用对应的方法，那么在
	 * LoadTimeWeaverAwareProcessor 中的 PostProcessBeforeInitialization 函数中完成了什么样的逻辑呢？
	 *  我们综合之前讲解的所有信息，将所有的相关的信息串联起来一起分析这个函数，
	 *  在 LoadTimeWeaverAwareProcessor 中 postProcessBeforeInitialization  函数中最开始的 if 判断注定这个后处理器只对
	 *  LoadTimeWeaverAware 类型bean 起作用，而纵观所有的 bean,实现 LoadTimeWeaver 接口的类只有 AspectJWeavingEnabler 。
	 *  当在 Spring 中调用 AspectJWeavingEnabler时，this.loadTimeWeaver 尚未被初始化，那么会直接调用 beanFactory.getBean
	 *  方法中获取对应的DefaultContextLoadTimeWeaver 类型的 bean ，并将其设置为 AspectJWeavingEnabler 类型的 bean 的 loadTimeWeaver 属性中
	 *  ，当然 AspectJWeavingEnabler 同样实现了 BeanClassLoaderAware 及 Ordered 接口，实现了 BeanClassLoaderAware 接口保证了
	 *  bean 初始化的时候调用AbstractAutowireCapableBeanFactory 的 invokeAwareMethods 的时候将 beanClassLoader 赋值给当前的
	 *  类，而实现了 Ordered 接口则保证了实例化 bean 的时当前 bean  会被最先初始化
	 *  而 DefaultContextLoaderTimeWeaver类又同时实现了 LoadTimeWeaver，BeanClassLoaderAware 以及 DisposableBean
	 *  其中 DisposableBean  接口保证了 bean 的销毁时会调用 destory 方法进行 bean 的清理，而 beanClassLoaderAware 接口则
	 *  保证在 bean 初始化调用 AbstractAutowireCapableBeanFactory 的 invokeAwareMethods 时调用 setBeanClassLoader 方法
	 *
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof LoadTimeWeaverAware) {
			LoadTimeWeaver ltw = this.loadTimeWeaver;
			if (ltw == null) {
				Assert.state(this.beanFactory != null,
						"BeanFactory required if no LoadTimeWeaver explicitly specified");
				ltw = this.beanFactory.getBean(
						ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class);
			}
			((LoadTimeWeaverAware) bean).setLoadTimeWeaver(ltw);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}

}
