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

package org.springframework.aop.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Utility class for handling registration of auto-proxy creators used internally
 * by the '{@code aop}' namespace tags.
 *
 * <p>Only a single auto-proxy creator can be registered and multiple tags may wish
 * to register different concrete implementations. As such this class delegates to
 * {@link AopConfigUtils} which wraps a simple escalation protocol. Therefore classes
 * may request a particular auto-proxy creator and know that class, <i>or a subclass
 * thereof</i>, will eventually be resident in the application context.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 * @see AopConfigUtils
 */
public abstract class AopNamespaceUtils {

	/**
	 * The {@code proxy-target-class} attribute as found on AOP-related XML tags.
	 */
	public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

	/**
	 * The {@code expose-proxy} attribute as found on AOP-related XML tags.
	 */
	private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";


	public static void registerAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	public static void registerAspectJAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {
		/**
		 * 注册或升级 AutoProxyCreator 定义 beanName 为
		 * @see org.springframework.aop.config.internalAutoProxyCreator 的 beanDefinition
 		 */
		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		// 对于 proxy-target-class 以及 expose-proxy 属性的处理
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		// 注册组件并通知，便于监听器做进一步处理
		// 其中 beanDefinition 的 className 为 AnnotationAwareAspectJAutoProxyCreator
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, Element sourceElement) {
		if (sourceElement != null) {
			boolean proxyTargetClass = Boolean.valueOf(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
			// 对于 proxy-target-class 属性的处理
			if (proxyTargetClass) {
				// proxy-target-class ：Spring AOP  部分使用 JDK 动态代理 或者 CGLIB 来为目标对象创建代理 ，建义尽量使用 JDK 的动态代理
				// 如果被代理的对象实现了至少一个接口，则会使用 JDK 动态代理，所有的该目标类型实现接口都将被代理，若该目标对象没有实现任何接口
				//  则创建一个 CGLIB 代理，如果你希望强制的使用 CGLIB 代理，例如 ：希望代理目标对象的所有方法，而不只是实现自接口的方法
				// 那也可以，但是需要考虑以下两个问题
				// 无法通知(advise) Final 方法，因为他们不能被覆写
				// 你需要将 CGLIB 二进制发行包入在 classpath 下面
				// 与之相较，JDK 本身就提供了动态代理，强制的使用 CGLIB 代理需要将下面的<aop:config> 的 proxy-target-class 属性设置为 true
				// <aop:config proxy-target-class="true"> ... </aop-config>
				// 当需要使用 CGLIB 代理和@AspectJ  自动代理支持，可以按照以下的方式设置，<aop:aspectj-autoproxy> 的 proxy-target-class 属性
				// <aop:aspectj-autoproxy proxy-target-class="true"/>
				// 而实际使用的过程才会发现细节问题的差别,the devil is in the detail
				// JDK 动态代理 ：其代理对象必需是某个接口的实现，它是通过在运行期间创建一个接口的实现类来完成对目标对象的代理
				// CGLIB  代理：实现原理类似于 JDK 动态代理的原理，只是它在运行期间生成的代理对象是针对目标类扩展子类，CGLIB 的高效代码
				// 生成包，底层是依靠 ASM(开源的 java 字节码编辑类库)操作字节码实现的，性能上比 JDK 要强
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
			// 对于 expose-proxy 属性的处理
			// expose-proxy ： 有个时候目标对象内部的自我调用将无法实施切面中拉绳，如下示例
			// public interface AService {
			//		public void a();
			//		public void b ();
			// }
			// @Service()
			// public class AServiceImpl implements AService {
			//		@Transactional (propagation=Propagation.REQUIRED)
			//		public void a (){
			//			this.b();
			//		}
			//		@Transactional(propagation=Propagation.REQUIRES_NEW)
			//		public void b (){
			//
			//		}
			// }
			// 此时的 this 指向的是目标对象，因此调用 this.b()将不会执行 b事务切面，即不会以执行事务增强，因此 b 方法的事务定义
			// @Transactional(propagation=Propagation.REQUIRES_NEW) 将不会实施，为了解决这个问题，我们可以这样做
			// <aop:aspectj-autoproxy expose-proxy="true"/>
			// 然后将上面的代码中的 this.b() 修改为((AService)AopContext.currentProxy).b(),即可，通过上面的修改便可以完成 a 和
			// b 方法同时增强
			// 最后注册组件并通知，便于监听器做进一步的处理，这里就不再说了
			boolean exposeProxy = Boolean.valueOf(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
			if (exposeProxy) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

	private static void registerComponentIfNecessary(BeanDefinition beanDefinition, ParserContext parserContext) {
		if (beanDefinition != null) {
			BeanComponentDefinition componentDefinition =
					new BeanComponentDefinition(beanDefinition, AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
			parserContext.registerComponent(componentDefinition);
		}
	}

}
