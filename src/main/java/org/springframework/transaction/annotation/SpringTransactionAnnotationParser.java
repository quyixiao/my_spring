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

package org.springframework.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy implementation for parsing Spring's {@link Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class SpringTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

	@Override
	public TransactionAttribute parseTransactionAnnotation(AnnotatedElement ae) {
		AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ae, Transactional.class);
		if (attributes != null) {
			return parseTransactionAnnotation(attributes);
		}
		else {
			return null;
		}
	}

	public TransactionAttribute parseTransactionAnnotation(Transactional ann) {
		return parseTransactionAnnotation(AnnotationUtils.getAnnotationAttributes(ann, false, false));
	}

	/***
	 *  至此，我们终于看到了想看到的获取注解标记的代码了，首先会判断当前的类是否含有Transactional注解，这个事务属性的基础，
	 *  当然如果有的话，会继续调用parseTransactionAnnotation方法解析详细的属性
	 *  上面方法中实现了对应的类或者方法的事务属性解析，你会在这个类中看到任何你常用或者不常用的属性提取，
	 *  至此，我们终于完成了事务标签的解析，我们是不是分析的太远了，似乎已经忘了从哪里开始了，回顾一下，我们现在的任务是找出某个增强器
	 *  是否适合于对应的类，而是否匹配关键在于是否从指定的类或类中的方法中找到对应的事务属性，现在，我们以UserServiceImpl 为例
	 *  ，已经在它的接口UserService 中找到了事务的属性，所以，它是与事务增强器匹配的，也就是它会被事务功能修饰的
	 *  至此，事务功能的初始化工作便已经结束了，当判断某个bean 适用于事务增强时，也就是适用于增强器BeanFactoryTransactionAttributeSourceAdvisor
	 *  ，没错，还是这个类，所以说，在自定义标签解析时，注入的类成为了整个事务的功能的基础
	 *  BeanFactoryTransactionAttributeSourceAdvisor 作为Advisor 的实现类，自然要遵从advisor的处理方式，当代理被调用时会调用这个类的
	 *  增强方法，也就是此bean 的advise ，又因为在解析事务定义标签时我们把TransactionInterceptor 类型的bean注入到了BeanFactoryTransactionAttributeSourceAdvisor 中
	 *  所以，在调用事务增强器增强的代理时会首先执行TransactionInterceptor进行增强，同时，也就是在TransactinInterceptor 类中的invoke
	 *  方法中完成了整个事务逻辑
	 *
	 */
	protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
		RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
		// 解析 propagation
		Propagation propagation = attributes.getEnum("propagation");
		rbta.setPropagationBehavior(propagation.value());
		// 解析isolation
		Isolation isolation = attributes.getEnum("isolation");
		rbta.setIsolationLevel(isolation.value());
		// 解析timeout
		rbta.setTimeout(attributes.getNumber("timeout").intValue());
		// 解析readOnly
		rbta.setReadOnly(attributes.getBoolean("readOnly"));
		// 解析 value
		rbta.setQualifier(attributes.getString("value"));
		ArrayList<RollbackRuleAttribute> rollBackRules = new ArrayList<RollbackRuleAttribute>();
		// 解析rollbackFor
		Class<?>[] rbf = attributes.getClassArray("rollbackFor");
		for (Class<?> rbRule : rbf) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		// 解析 rollbackForClassName
		String[] rbfc = attributes.getStringArray("rollbackForClassName");
		for (String rbRule : rbfc) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		// 解析noRollbackFor
		Class<?>[] nrbf = attributes.getClassArray("noRollbackFor");
		for (Class<?> rbRule : nrbf) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		// 解析 noRollbackForClassName
		String[] nrbfc = attributes.getStringArray("noRollbackForClassName");
		for (String rbRule : nrbfc) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		rbta.getRollbackRules().addAll(rollBackRules);
		return rbta;
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof SpringTransactionAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringTransactionAnnotationParser.class.hashCode();
	}

}
