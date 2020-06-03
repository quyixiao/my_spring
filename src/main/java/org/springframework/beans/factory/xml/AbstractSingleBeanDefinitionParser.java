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

package org.springframework.beans.factory.xml;


import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Base class for those {@link BeanDefinitionParser} implementations that
 * need to parse and define just a <i>single</i> {@code BeanDefinition}.
 *
 * <p>Extend this parser class when you want to create a single bean definition
 * from an arbitrarily complex XML element. You may wish to consider extending
 * the {@link AbstractSimpleBeanDefinitionParser} when you want to create a
 * single bean definition from a relatively simple custom XML element.
 *
 * <p>The resulting {@code BeanDefinition} will be automatically registered
 * with the {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 * Your job simply is to {@link #doParse parse} the custom XML {@link Element}
 * into a single {@code BeanDefinition}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 * @see #getBeanClass
 * @see #getBeanClassName
 * @see #doParse
 */
public abstract class AbstractSingleBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * Creates a {@link BeanDefinitionBuilder} instance for the
	 * {@link #getBeanClass bean Class} and passes it to the
	 * {@link #doParse} strategy method.
	 * @param element the element that is to be parsed into a single BeanDefinition
	 * @param parserContext the object encapsulating the current state of the parsing process
	 * @return the BeanDefinition resulting from the parsing of the supplied {@link Element}
	 * @throws IllegalStateException if the bean {@link Class} returned from
	 * {@link #getBeanClass(org.w3c.dom.Element)} is {@code null}
	 * @see #doParse
	 *  回顾一下全部的自定义标签处理过程，虽然在实例中我们定义的 UserBeanDefinitionParser，但是其中我们只做了与自己业务逻辑相关的部分
	 *  ,不过，我们没有做，但是并不是代表没有，在这个处理过程中同样的是按照 Spring 中默认的标签处理方式进行的，包括创建 BeanDefintion
	 *  以及进行相应的默认的属性的设置，对于这些工作，Spring都默默的帮我们实现了，只是暴露出一些接口，来借用户实现修改化业务，通过对本章的
	 *  了解，相信读者对 Spring 中自定义标签的使用以及在解析自定义标签的过程中Spring 为我们做了哪些工作会有一个全面的了解，到此，我们已经完成
	 *   完成了 Spring 中全部的解析工作，也就是说到现在为止我们已经理解 了 Spring 将 bean 从配置文件中加载到内存中的全部过程，而接下来的
	 *   任务就是如何的使用这些 bean,下一章节你介绍 bean 的加载
	 *
	 */
	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		String parentName = getParentName(element);
		if (parentName != null) {
			builder.getRawBeanDefinition().setParentName(parentName);
		}
		// 获取自定义标签中的 class,此时会调用自定义解析器如： UserBeanDefinitionParser中的 getBeanClass 方法
		// 		 	//Elment 对应的类
		//						 	protected Class getBeanClass(Elment element){
		//							return User.class;
		//						 	}
		// 获取User.class 对象
		Class<?> beanClass = getBeanClass(element);
		if (beanClass != null) {
			builder.getRawBeanDefinition().setBeanClass(beanClass);
		}

		else {
			// 如果子类没有重写 getBeanClass 方法，则尝试检查子类 是否重写了 getBeanClassName 方法
			String beanClassName = getBeanClassName(element);
			if (beanClassName != null) {
				builder.getRawBeanDefinition().setBeanClassName(beanClassName);
			}
		}
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		if (parserContext.isNested()) {
			// 如果存在父类则使用父类的 scope 属性
			// Inner bean definition must receive same scope as containing bean.
			builder.setScope(parserContext.getContainingBeanDefinition().getScope());
		}
		if (parserContext.isDefaultLazyInit()) {
			// Default-lazy-init applies to custom bean definitions as well.
			// 配置了延迟加载
			builder.setLazyInit(true);
		}

		// 调用子类 重写的 doParse 方法进行解析
		doParse(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

	/**
	 * Determine the name for the parent of the currently parsed bean,
	 * in case of the current bean being defined as a child bean.
	 * <p>The default implementation returns {@code null},
	 * indicating a root bean definition.
	 * @param element the {@code Element} that is being parsed
	 * @return the name of the parent bean for the currently parsed bean,
	 * or {@code null} if none
	 */
	protected String getParentName(Element element) {
		return null;
	}

	/**
	 * Determine the bean class corresponding to the supplied {@link Element}.
	 * <p>Note that, for application classes, it is generally preferable to
	 * override {@link #getBeanClassName} instead, in order to avoid a direct
	 * dependence on the bean implementation class. The BeanDefinitionParser
	 * and its NamespaceHandler can be used within an IDE plugin then, even
	 * if the application classes are not available on the plugin's classpath.
	 * @param element the {@code Element} that is being parsed
	 * @return the {@link Class} of the bean that is being defined via parsing
	 * the supplied {@code Element}, or {@code null} if none
	 * @see #getBeanClassName
	 */
	protected Class<?> getBeanClass(Element element) {
		return null;
	}

	/**
	 * Determine the bean class name corresponding to the supplied {@link Element}.
	 * @param element the {@code Element} that is being parsed
	 * @return the class name of the bean that is being defined via parsing
	 * the supplied {@code Element}, or {@code null} if none
	 * @see #getBeanClass
	 */
	protected String getBeanClassName(Element element) {
		return null;
	}

	/**
	 * Parse the supplied {@link Element} and populate the supplied
	 * {@link BeanDefinitionBuilder} as required.
	 * <p>The default implementation delegates to the {@code doParse}
	 * version without ParserContext argument.
	 * @param element the XML element being parsed
	 * @param parserContext the object encapsulating the current state of the parsing process
	 * @param builder used to define the {@code BeanDefinition}
	 * @see #doParse(Element, BeanDefinitionBuilder)
	 */
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		doParse(element, builder);
	}

	/**
	 * Parse the supplied {@link Element} and populate the supplied
	 * {@link BeanDefinitionBuilder} as required.
	 * <p>The default implementation does nothing.
	 * @param element the XML element being parsed
	 * @param builder used to define the {@code BeanDefinition}
	 */
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
	}

}
