/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.util.ClassUtils;

/**
 * Parser for the &lt;context:load-time-weaver/&gt; element.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
class LoadTimeWeaverBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String WEAVER_CLASS_ATTRIBUTE = "weaver-class";

	private static final String ASPECTJ_WEAVING_ATTRIBUTE = "aspectj-weaving";

	private static final String DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME =
			"org.springframework.context.weaving.DefaultContextLoadTimeWeaver";

	private static final String ASPECTJ_WEAVING_ENABLER_CLASS_NAME =
			"org.springframework.context.weaving.AspectJWeavingEnabler";


	/***
	 * 单凭以上的信息我们至少可以推断，当 Spring 在读取到自定义标签<context:load-time-weaver/> 后会生产一个 bean ，而这个 bean
	 * 的 id 为 loadTimeWeaver，class 为 org.springframework.context.weaving.DefaultContextLoadTimeWeaver，也就是完成了
	 * DefaultContextLoadTimeWeaver 类来注册
	 * 完成了以后的注册功能后，并不意味着这在 Spring 中就可以使用 AspectJ 了，因为我们还有一个很重要的步骤忽略了，就是 LoadTimeWeaverAwareProcessor 注册
	 * ，在 AbstractApplicationContext 中的 prepareBeanFactory  函数中有这样一段代码
	 *
	 */
	@Override
	protected String getBeanClassName(Element element) {
		if (element.hasAttribute(WEAVER_CLASS_ATTRIBUTE)) {
			return element.getAttribute(WEAVER_CLASS_ATTRIBUTE);
		}
		return DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME;
	}

	/****
	 *  继续跟进 LoadTimeWEAverBeanDefinitionParser ，作为BeanDefinitionParser 接口的实现类，他们的核心逻辑是从 parse 函数开始的
	 *  而经过父类的封装，LoadTimeWeaverBeanDefinitionParser 类的核心实现被转移到了 doParse函数中，如下：
	 *
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// ASPECTJ_WEAVING_ENABLER_CLASS_NAME=org.springframework.context.weaving.AspectJWeavingEnabler
		// 其实在之前的分析动态 Aop 也就是在分析配置<aop:aspectj-autoproxy/> 中已经提到了自定义配置解析流程
		// 对于<aop:aspectj-autoproxy/> 的解析无非是以标签作为标志，进而进行相关的处理类的注册，那么对于自定义标签
		// <context:load-time-weaver /> 其实是起到了同样的作用的，
		// 上面的函数的核心作用其实就是注册一个对于 AspectJ 处理类 org.Springframework.context.weaving.AspectJWeavingEnabler
		// ，它的注册总结起来如下：
		// 是否才开启了 AspectJ
		// 之前虽然反复的反映到了配置文件中加入了<context:load-time-weaver/> 便相当于加入了 Aspect开关，但是并不是配置了这个标签
		// 就意味着开启了 AspectJ 的功能，这个标签还还有一个属性 aspectj-weaving ,这个属性有3个备选值，on,off,和 autodect , 默认的
		// autodetect，也就是说，如果我们使用了<context:load-time-weaver/> ，那么 Spring  会帮我们检测是否可以使用 AspectJ 的功能
		// 而检测的依据便是文件中 META-INF/aop.xml 是否存在，看看 Spring 中的实现方式
		//  将 org.springframwwork.context.weaving.AspectJWeavingEnabler 封装在 BeanDefinition 中注册
		// 当通过 AspectJ 功能验证后便可以进行 AspectJWeavingEnabler 的注册了，注册方式很简单，无非是将类路径注册在新的初始化的 RootBeanDefinition 中
		//  在 RootBeanDefinition 的获取时会转换成对应的 class
		// 尽管在 init 方法中注册了 AspectWeavingEnabler 但是对于标签本身 Spring 也会以 bean 的形式保存，也就是当 Spring  解析到
		// <context:load-time-weaver/> 标签的时候也会产生一个 bean ，而这个 bean 中信息是什么呢？
		if (isAspectJWeavingEnabled(element.getAttribute(ASPECTJ_WEAVING_ATTRIBUTE), parserContext)) {
			RootBeanDefinition weavingEnablerDef = new RootBeanDefinition();

			weavingEnablerDef.setBeanClassName(ASPECTJ_WEAVING_ENABLER_CLASS_NAME);
			parserContext.getReaderContext().registerWithGeneratedName(weavingEnablerDef);

			if (isBeanConfigurerAspectEnabled(parserContext.getReaderContext().getBeanClassLoader())) {
				new SpringConfiguredBeanDefinitionParser().parse(element, parserContext);
			}
		}
	}

	protected boolean isAspectJWeavingEnabled(String value, ParserContext parserContext) {
		if ("on".equals(value)) {
			return true;
		}
		else if ("off".equals(value)) {
			return false;
		}
		else {
			// Determine default...
			// 自动检测
			ClassLoader cl = parserContext.getReaderContext().getResourceLoader().getClassLoader();
			return (cl.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) != null);
		}
	}

	protected boolean isBeanConfigurerAspectEnabled(ClassLoader beanClassLoader) {
		return ClassUtils.isPresent(SpringConfiguredBeanDefinitionParser.BEAN_CONFIGURER_ASPECT_CLASS_NAME,
				beanClassLoader);
	}

}
