/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanExpressionException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Standard implementation of the
 * {@link BeanExpressionResolver}
 * interface, parsing and evaluating Spring EL using Spring's expression module.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.expression.ExpressionParser
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 * @see org.springframework.expression.spel.support.StandardEvaluationContext
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver {

	/** Default expression prefix: "#{" */
	/** 默认表达式前缀 */
	public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

	/** Default expression suffix: "}" */
	/** 默认表达式后缀 */
	public static final String DEFAULT_EXPRESSION_SUFFIX = "}";

	/**
	 *  表达式前缀
	 */
	private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;
	/**
	 *  表达式后缀
	 */
	private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;
	/**
	 *  表达式解析器
	 */
	private ExpressionParser expressionParser;
	/**
	 *  表达式缓存
	 */
	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<String, Expression>(256);
	/**
	 *  评估缓存
	 */
	private final Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache =
			new ConcurrentHashMap<BeanExpressionContext, StandardEvaluationContext>(8);
	/**
	 *  解析上下文
	 */
	private final ParserContext beanExpressionParserContext = new ParserContext() {
		@Override
		public boolean isTemplate() {
			return true;
		}
		@Override
		public String getExpressionPrefix() {
			return expressionPrefix;
		}
		@Override
		public String getExpressionSuffix() {
			return expressionSuffix;
		}
	};


	/**
	 * Create a new {@code StandardBeanExpressionResolver} with default settings.
	 */
	public StandardBeanExpressionResolver() {
		this.expressionParser = new SpelExpressionParser();
	}

	/**
	 * Create a new {@code StandardBeanExpressionResolver} with the given bean class loader,
	 * using it as the basis for expression compilation.
	 * @param beanClassLoader the factory's bean class loader
	 */
	public StandardBeanExpressionResolver(ClassLoader beanClassLoader) {
		this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
	}


	/**
	 * Set the prefix that an expression string starts with.
	 * The default is "#{".
	 * @see #DEFAULT_EXPRESSION_PREFIX
	 */
	public void setExpressionPrefix(String expressionPrefix) {
		Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
		this.expressionPrefix = expressionPrefix;
	}

	/**
	 * Set the suffix that an expression string ends with.
	 * The default is "}".
	 * @see #DEFAULT_EXPRESSION_SUFFIX
	 */
	public void setExpressionSuffix(String expressionSuffix) {
		Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
		this.expressionSuffix = expressionSuffix;
	}

	/**
	 * Specify the EL parser to use for expression parsing.
	 * <p>Default is a {@link org.springframework.expression.spel.standard.SpelExpressionParser},
	 * compatible with standard Unified EL style expression syntax.
	 */
	public void setExpressionParser(ExpressionParser expressionParser) {
		Assert.notNull(expressionParser, "ExpressionParser must not be null");
		this.expressionParser = expressionParser;
	}


	@Override
	public Object evaluate(String value, BeanExpressionContext evalContext) throws BeansException {
		// value 为 null 或空字符串
		if (!StringUtils.hasLength(value)) {
			return value;
		}
		try {
			// 尝试从缓存中读取指定的表达式
			Expression expr = this.expressionCache.get(value);
			if (expr == null) {
				// 使用表达式解析器解析此表达式
				expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
				// 加入缓存
				this.expressionCache.put(value, expr);
			}
			// 读取表达式解析上下文
			StandardEvaluationContext sec = this.evaluationCache.get(evalContext);
			if (sec == null) {
				// 初始化解析上下文
				sec = new StandardEvaluationContext();
				sec.setRootObject(evalContext);
				// 加载一系列的属性访问器
				sec.addPropertyAccessor(new BeanExpressionContextAccessor());
				sec.addPropertyAccessor(new BeanFactoryAccessor());
				sec.addPropertyAccessor(new MapAccessor());
				sec.addPropertyAccessor(new EnvironmentAccessor());
				sec.setBeanResolver(new BeanFactoryResolver(evalContext.getBeanFactory()));
				sec.setTypeLocator(new StandardTypeLocator(evalContext.getBeanFactory().getBeanClassLoader()));
				// 类型转换服务不为空，则将其写入 StandardEvaluationContext 中
				ConversionService conversionService = evalContext.getBeanFactory().getConversionService();
				if (conversionService != null) {
					sec.setTypeConverter(new StandardTypeConverter(conversionService));
				}
				// 允许子类实现自定义配置的钩子函数
				customizeEvaluationContext(sec);
				// 写入缓存
				this.evaluationCache.put(evalContext, sec);
			}
			// 在标准的解析上下文中解析此表达式
			return expr.getValue(sec);
		}
		catch (Exception ex) {
			throw new BeanExpressionException("Expression parsing failed", ex);
		}
	}

	/**
	 * Template method for customizing the expression evaluation context.
	 * <p>The default implementation is empty.
	 */
	protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
	}

}
