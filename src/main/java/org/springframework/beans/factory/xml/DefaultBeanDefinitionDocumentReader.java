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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.test.LogUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.*;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	private XmlReaderContext readerContext;

	private BeanDefinitionParserDelegate delegate;


	/**
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 * 根据Spring DTD对Bean定义规则解析Bean定义的文档对象
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		//获取XML描述符
		this.readerContext = readerContext;
		logger.debug("Loading bean definitions");
		// 获取Document的根元素
		Element root = doc.getDocumentElement();
		NamedNodeMap attributes = root.getAttributes();

		LogUtils.info("registerBeanDefinitions length :"  + attributes.getLength());

		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			LogUtils.info("registerBeanDefinitions node name :" + node.getNodeName());
		}

		doRegisterBeanDefinitions(root);
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor} to pull the
	 * source metadata from the supplied {@link Element}.
	 */
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * Register each bean definition within the given root {@code <beans/>} element.
	 */
	protected void doRegisterBeanDefinitions(Element root) {
		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		// 具体的解析过程由BeanDefinitionParserDelegate实现
		// BeanDefinitionParserDelegate 中定义了Spring Bean定义的XML文件中的各种元素
		BeanDefinitionParserDelegate parent = this.delegate;
		//
		this.delegate = createDelegate(getReaderContext(), root, parent);
		if (this.delegate.isDefaultNamespace(root)) {
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					return;
				}
			}
		}
		// 在解析Bean定义之前，进行自定义解析，增强解析过程的扩展性
		preProcessXml(root);
		// 从文档的根元素开始进行bean定义的文档对象来解析
		parseBeanDefinitions(root, this.delegate);
		// 在解析Bean定义之后进行自定义解析，增加解析过程的可扩展性
		postProcessXml(root);

		this.delegate = parent;
	}
	// 创建BeanDefinitionParserDelegate，用于完成真正的解析过程
	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		// BeanDefinitionParserDelegate初始化Document根元素
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * @param root the DOM root element of the document
	 * 使用Spring的Bean规则从文档的根元素开始Bean定义的文档对象解析
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		// Bean 的定义的文档对象使用了Spring默认的命名空间
		if (delegate.isDefaultNamespace(root)) {
			// 获取Bean定义的文档对象根元素的所有子节点
			NodeList nl = root.getChildNodes();
			LogUtils.info("parseBeanDefinitions root child length :" +nl.getLength() );
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					// 获取文档节点的XML元素的节点
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						// 使用Spring的Bean规则解析元素节点
						parseDefaultElement(ele, delegate);
					}
					else {
						// 如果没有使用Spring默认的xml命名空间，则使用用户自定义的解析规则解析元素的节点
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {
			// 文档对象根节点没有使用Spring默认的命名空间
			// 使用自定义的解析规则解析文档的根节点
			delegate.parseCustomElement(root);
		}
	}

	// 使用Spring的Bean规则 解析文档元素的节点
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// 如果节点是<import>导入的元素，进行导入解析
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}
		// 如果节点是<alaas>别名元素，进行别名解析
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		// 如果节点既不是导入元素，也不是别名元素，而是普通的<bean>元素，则按照Spring的Bean规则解析元素
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			LogUtils.all("parseDefaultElement delegate bean ");
			processBeanDefinition(ele, delegate);
		}
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 * 解析<import>导入的元素，从给定的导入路径中加载Bean资源到Spring IOC 容器中
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// 获取给定的导入元素的location属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		LogUtils.info(" importBeanDefinitionResource location  :" + location);
		//如果导入元素的location属性值为空，则没有导入任何资源，直接返回
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		// 使用系统变量值解析location属性值
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<Resource>(4);

		// Discover whether the location is an absolute or relative URI
		// 标识给定导入元素的location属性值是否绝对路径。
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// 给定导入元素的location属性值不是绝对路径
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		// 给定导入元素的location属性值是绝对路径
		if (absoluteLocation) {
			try {
				// 使用资源读入器加载给定路径的bean资源
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {
			// No URL -> considering resource location as relative to the current file.
			// 给定导入元素的location属性值是相对路径
			try {
				int importCount;
				// 将给定的导入元素的location封装为相对路径资源
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				// 封装的相对路径资源存在
				if (relativeResource.exists()) {
					// 使用资源读入器加载Bean的资源
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				// 封装的相对路径资源不存在
				else {
					// 获取Spring IoC容器资源读入器的基本路径
					String baseLocation = getReaderContext().getResource().getURL().toString();
					// 根据Spring IOc容器读入器的基本路加载给定导入路径的资源路径
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
						ele, ex);
			}
		}
		Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
		// 在解析完成<import>元素之后，发送容器导入其他资源处理完成事件
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 *  解析<alias>别名元素，为Bean向Spring IoC容器注册别名
	 *
	 */
	protected void processAliasRegistration(Element ele) {
		//获取<alias>别名元素中的name的属性值
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		//获取<alias>别名元素中的alias的属性值
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		//<alias>别名元素的alias属性值为空
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		//<alias>别名元素的alias属性值为空
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}

		if (valid) {
			try {
				// 向容器的资源读入器注册别名
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// 在解析完<alias>元素之后，发送容器别名处理完成事件
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 * 解析Bean资源文档对象的普通元素
	 * 通过上述的Spring 容器对载入的Bean定义文档解析可以看出,Spring配置文件中可以使用<inport>元素来导入IoC容器所需要的其他资源，
	 * SpringIoc容器在解析时首先将指定的资源加载到容器中，使用<alias>别名时，Spring Ioc容器首先将别名元素所定义的别名注册到容器中，
	 * 对于既不是<import>元素又不是<alias>的元素，即Spring配置文件中普通的<bean>元素，由BeanDefinitionParserDeletegate类的
	 * ParseBeanDefinitionElement()方法实现解析，这个解析过程非常的复杂
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		//
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		// BeanDefinitionHolder是对BeanDefinition的封装，即Bean定义封装类
		// 对文档中的<Bean>元素解析由BeanDefinitionParserDelegate实现
		// BeanDefinitionHolder bdHolder = deletate.parseBeanDefinitionElement(element)
		// BeanDefinitionHolder是对
		if (bdHolder != null) {
			LogUtils.info("processBeanDefinition bdHolder is not null ,bdHolder Name :" + bdHolder.getClass().getName());
			/***
			 * 如果需要的话就对 beanDefinition 进行装饰，那么这句代码的作用就是什么功能呢？
			 * 这句代码的使用场景如下：
			 * <bean id="test" class="test.MyClass">
			 *     <mybean:user username="aaaa"/>
			 * </bean>
			 * 当 Spring 中的 bean 使用了默认的标签配置，但是其中的子元素却使用了自定义的配置，这句代码就起作用了，可能会有人会疑问，
			 * 之前讲过，对 bean 的解析分成两种类型，一种是默认的类型解析，另一种是自定义类型解析，这不正是自定义类型解析吗？为什么会在
			 * 默认的类型解析中单独的添加一个方法的处理呢，确实，这个问题很让人迷惑，但是，不知道聪明的读者有没有发现，这个自定义类型并不是
			 * 以 Bean 的形式出现的呢？我们之前讲过两种类型的不同处理只是针对 bean 的，这里我们看到，这个自定义类型其实是属性，好了，我们
			 * 我们继续分析这个代码
			 */
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				// 向Spring Ioc容器注册解析得到的bean定义，这是Bean定义向Ioc容器注册的入口
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event.
			// 在完成向Spring IoC容器注册解析得到的Bean定义之后，发送注册事件
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {

		LogUtils.info("preProcessXml");

	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
		LogUtils.info("postProcessXml ");
	}

}
