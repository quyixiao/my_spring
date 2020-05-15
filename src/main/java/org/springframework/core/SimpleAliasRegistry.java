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

package org.springframework.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Simple implementation of the {@link AliasRegistry} interface.
 * Serves as base class for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/** Map from alias to canonical name */
	private final Map<String, String> aliasMap = new ConcurrentHashMap<String, String>(16);

	/***
	 * 在对 bean 进行定义时，除了使用 id属性来指定名称之外，为了提供了名称，可以使用 alias 标签来指定，而所有的
	 *  这些名称都指向同一个 bean,在某些情况下，提供的别名非常的有用，比如了为让每个引用更加的容易的对公共的组件 进行引用
	 *  然而，在定义 bean时就指定所有的别名并不是总是恰当的，有时， 我们期望能在当前位置为那些在别处的 bean 定义 bean引入别名
	 *  在 xml 配置文件中，可用单独的 alias元素来完成 bean 别名定义，如配置文件中的定义一个 javaBean
	 *  <bean id="testBean" class="com.test"></bean>
	 *  要给这个 javaBean 增加别名，以方便不同的对象来引用 ，我们就可以直接的使用 bean 标签中的 name 属性
	 *  <bean id="testBean" name = "testBean,testBean2" class="com.test"></bean>
	 *  同样 Spring 还有另一个种声明别名的方式
	 *  <bean id="testBean" class="com.test"></bean>
	 *  <bean name = "testBean" alias = "testBean,testBean2"></bean>
	 *
	 * 	考虑一个更加具体的例子，组件 A 在 XML 配置文件中定义了一个名字为 componetA 的 Datasorce 类型的 bean,但是组件 B 却想在其
	 * 	XML 文件中以 componetB 命名来引用此 bean ，而且在主程序 MyApp 中的 xml 配置文件中，希望以 myApp 的名字来引用些类型的 bean,
	 * 	最后，容器加载3个 XML 文件来生成最终的 ApplicationContext,在些情形下，可以通过配置文件中添加下列的 alias 元素来实现
	 * 	<alias name="componentA" alias="componentB" />
	 * 	<alias name="componentA" alias="myApp"/>
	 *
	 *
	 *  下面的代码中可以得到注册的 alias 步骤如下：
	 *  1.alias 与 beanName 相同的情况处理，若 alias 与 BeanName并名称相同则不需要处理并删除掉原有的 alias
	 *  2.alias 覆盖处理，若 aliasName 已经使用并已经指向了另一个 BeanName，则需要用户的设置进行处理
	 *  3.alias 循环检查，当 A->B 存在时，若再次出现 A->C->B  时候则会抛出异常
	 *  注册 alias
	 */
	@Override
	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		//如果 beanName 与 alias 相同的话，就不再记录 alias，并删除对应的 alias
		if (alias.equals(name)) {
			this.aliasMap.remove(alias);
		}
		else {

			String registeredName = this.aliasMap.get(alias);
			if (registeredName != null) {
				if (registeredName.equals(name)) {
					// An existing alias - no need to re-register
					return;
				}
				//如果 alias 不允许被覆盖则抛出异常
				if (!allowAliasOverriding()) {
					throw new IllegalStateException("Cannot register alias '" + alias + "' for name '" +
							name + "': It is already registered for name '" + registeredName + "'.");
				}
			}
			// 当 A-> B 存在时，若再次出现 A->C->B 时候，则抛出异常
			checkForAliasCircle(name, alias);
			this.aliasMap.put(alias, name);
		}
	}

	/**
	 * Return whether alias overriding is allowed.
	 * Default is {@code true}.
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	/**
	 * Determine whether the given name has the given alias registered.
	 * @param name the name to check
	 * @param alias the alias to look for
	 * @since 4.2.1
	 */
	public boolean hasAlias(String name, String alias) {
		for (Map.Entry<String, String> entry : this.aliasMap.entrySet()) {
			String registeredName = entry.getValue();
			if (registeredName.equals(name)) {
				String registeredAlias = entry.getKey();
				return (registeredAlias.equals(alias) || hasAlias(registeredAlias, alias));
			}
		}
		return false;
	}

	@Override
	public void removeAlias(String alias) {
		String name = this.aliasMap.remove(alias);
		if (name == null) {
			throw new IllegalStateException("No alias '" + alias + "' registered");
		}
	}

	@Override
	public boolean isAlias(String name) {
		return this.aliasMap.containsKey(name);
	}

	@Override
	public String[] getAliases(String name) {
		List<String> result = new ArrayList<String>();
		synchronized (this.aliasMap) {
			retrieveAliases(name, result);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * Transitively retrieve all aliases for the given name.
	 * @param name the target name to find aliases for
	 * @param result the resulting aliases list
	 */
	private void retrieveAliases(String name, List<String> result) {
		for (Map.Entry<String, String> entry : this.aliasMap.entrySet()) {
			String registeredName = entry.getValue();
			if (registeredName.equals(name)) {
				String alias = entry.getKey();
				result.add(alias);
				retrieveAliases(alias, result);
			}
		}
	}

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * @param valueResolver the StringValueResolver to apply
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			Map<String, String> aliasCopy = new HashMap<String, String>(this.aliasMap);
			for (String alias : aliasCopy.keySet()) {
				String registeredName = aliasCopy.get(alias);
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (resolvedAlias == null || resolvedName == null || resolvedAlias.equals(resolvedName)) {
					this.aliasMap.remove(alias);
				}
				else if (!resolvedAlias.equals(alias)) {
					String existingName = this.aliasMap.get(resolvedAlias);
					if (existingName != null) {
						if (existingName.equals(resolvedName)) {
							// Pointing to existing alias - just remove placeholder
							this.aliasMap.remove(alias);
							break;
						}
						throw new IllegalStateException(
								"Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias +
								"') for name '" + resolvedName + "': It is already registered for name '" +
								registeredName + "'.");
					}
					checkForAliasCircle(resolvedName, resolvedAlias);
					this.aliasMap.remove(alias);
					this.aliasMap.put(resolvedAlias, resolvedName);
				}
				else if (!registeredName.equals(resolvedName)) {
					this.aliasMap.put(alias, resolvedName);
				}
			}
		}
	}

	/**
	 * Check whether the given name points back to the given alias as an alias
	 * in the other direction already, catching a circular reference upfront
	 * and throwing a corresponding IllegalStateException.
	 * @param name the candidate name
	 * @param alias the candidate alias
	 * @see #registerAlias
	 * @see #hasAlias
	 */
	protected void checkForAliasCircle(String name, String alias) {
		if (hasAlias(alias, name)) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Circular reference - '" +
					name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

	/**
	 * Determine the raw name, resolving aliases to canonical names.
	 * @param name the user-specified name
	 * @return the transformed name
	 * //再进入canonicalName方法查看
	 * //此方法在SimpleAliasRegistry中实现，被默认bean工厂间接继承
	 * 确定原生的name，将别名解析为BeanName
	 *
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		// 处理别名
		String resolvedName;
		do {
			//拿到canonicalName对应的实际名称
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				canonicalName = resolvedName;
			}
		}

		//只有当canonicalName在aliasMap中对应的value为null时，才跳出循环
		//这时候说明canonicalName已经不作为其他任何BeanName的别名，排除了间接引用
		//canonicalName就为真正的beanName
		while (resolvedName != null);
		return canonicalName;
	}

}
