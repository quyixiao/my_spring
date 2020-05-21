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

package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} allowing for the configuration of
 * declarative transaction management using either XML or using annotations.
 *
 * <p>This namespace handler is the central piece of functionality in the
 * Spring transaction management facilities and offers two approaches
 * to declaratively manage transactions.
 *
 * <p>One approach uses transaction semantics defined in XML using the
 * {@code &lt;tx:advice&gt;} elements, the other uses annotations
 * in combination with the {@code &lt;tx:annotation-driven&gt;} element.
 * Both approached are detailed to great extent in the Spring reference manual.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class TxNamespaceHandler extends NamespaceHandlerSupport {

	static final String TRANSACTION_MANAGER_ATTRIBUTE = "transaction-manager";

	static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";


	static String getTransactionManagerName(Element element) {
		return (element.hasAttribute(TRANSACTION_MANAGER_ATTRIBUTE) ?
				element.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE) : DEFAULT_TRANSACTION_MANAGER_BEAN_NAME);
	}


	/***
	 * 10.2  事务自定义标签
	 *  对于 Spring 中的事务功能代码分析，我们首先从配置文件中开始入手，在配置文件中会有这样的一个配置<tx:annotation-driver/>
	 *  可以说此处的配置是事务的开头，如果没有此配置，那么 Spring 中将不存在事务的功能，那么我们就从这个配置开始分析
	 *  根据之前的分析，我们因此可以判断，在自定义标签中的解析过程中一定是做了一些辅助的操作的，于是我们先从自定义的标签入手进行分析
	 *
	 */
	@Override
	public void init() {
		registerBeanDefinitionParser("advice", new TxAdviceBeanDefinitionParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
		registerBeanDefinitionParser("jta-transaction-manager", new JtaTransactionManagerBeanDefinitionParser());
	}

}
