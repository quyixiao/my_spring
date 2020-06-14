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
 * 通过实现接口BeanDefinitionDocumentReader的DefaultBeanDefinitionDocumentReader类对Document进行解析，并使用BeanDefinitionParserDelegate
 * 对Element进行解析
 *
 *
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
		LogUtils.info("Loading bean definitions");
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
			// 处理profile属性
			// 我们注意到在注册Bean的最开妈的是对PROFILE_ATTRIBUTE属性的解析，可能对于我们来说，profile属性并不是很常用，让我们先了解
			// 一下这个属性
			// 分析profile前我们先了解下profile的用法，官方示例代码片段如下
			/**
			 * <?xml version="1.0" encoding="UTF-8"?>
			 * <beans xmlns="http://www.springframework.org/schema/beans"
			 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd"
			 *	>
			 *	<beans profile="dev">
			 *	   ...
			 *	   <bean profile="production">
			 *	   	...
			 *	   </bean>
			 *  </beans>
			 *  集成到Web环境中时，在web.xml中加入以下代码
			 *  <context-param>
			 *      <param-name>Spring.profiles.active</param-name>
			 *      <param-value>dev</param-value>
			 *  </context-param>
			 *  有了这个†拨，我们就可以同时在配置文件中上部署两套配置来适用于生产环境和开发环境，这样可以方便的切换开发，部署环境
			 *  最常用的就是更换不同的数据库
			 *  了解了profile的使用再来分析代码清晰多了，首先程序会获取beans的节点是否定义了profile属性，如果定义了，则会需要到环境变量中
			 *  去寻找，所以这里首先断言environment不可能为空。因为profile是可以同时指定多个的，需要程序对其拆分，并解析每个profile都是
			 *  符合环境变量中所定义的，不定义则不会浪费性能去解析
			 *
			 */
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					return;
				}
			}
		}
		// 在解析Bean定义之前，进行自定义解析，增强解析过程的扩展性 | 留给子类实现
		preProcessXml(root);
		// 从文档的根元素开始进行bean定义的文档对象来解析
		parseBeanDefinitions(root, this.delegate);
		// 在解析Bean定义之后进行自定义解析，增加解析过程的可扩展性 | 留给子类实现
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
						// 使用Spring的Bean规则解析元素节点 | 开始默认标签解析
						// | 如 这个代码看起来逻辑还是蛮清晰的，因为在Spring 的XML配置里面有两大类Bean声明，一个是默认的，如
						// <bean id="test" class="test.TestBean">
						parseDefaultElement(ele, delegate);
					}
					else {
						// 如果没有使用Spring默认的xml命名空间，则使用用户自定义的解析规则解析元素的节点 |  开始自定义标签两种格式的区分
						// Spring 拿到一个元素时首先要做的是根据命名空间进行解析，如果是默认的命名空间，则使用 parseDefaultElement方法
						// 进行元素解析，否则使用 parseCustomElment 元素进行解析，在分析自定义标签的解析过程前，我们先了解一下使用过程
						// 在很多的情况下，我们需要为系统提供可配置的支持，简单的做法可以直接基于 Spring 的标准来配置，但是配置较为复杂
						// 的时候，解析工作是一个不得不考虑的负担，Spring 提供了可扩展的 Schema 的支持，这是一个不错的折中方案，扩展
						// Spring 自定义的标签配置大致需要以下的几个步骤，前提是把 Spring 的 core 包加入到项目中
						// 1.创建需要扩展的组件
						// 2.定义一个 xsd 文件描述组件内容
						// 3.创建一个文件，实现 BeanDefinitionParser接口，用来解析 xsd 文件中的定义和组件定义
						// 4.创建一个 Handler文件，扩展自NamespaceHandlerSupport，目的是将组件注册到 Spring 容器中
						// 编写 Spring.handlers和 Spring.schemas
						/***
						 * public class User{
						 *   private String userName;
						 *   private String email;
						 * }
						 * 定义一个 XSD 文件描述组件内容
						 * <?xml version="1.0" encoding="UTF-8"?>
						 * <beans xmlns="http://www.springframework.org/schema/beans"
						 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						 *        targetNamespace="http://www.lexueba.com/schema/user"
						 *        xmlns:tns="http://www.lexueba.com/schema/user"
						 *        elementFormDefault="qualified">
						 *        <elment name="user">
						 *         		<complexType>
						 *         		 	<attribute name="id" type="string" />
						 *         		 	<attribute name="userName" type="string"/>
						 *         		 	<attribute name="email" type="string"></attribute>
						 *         		</complexType>
						 *         </elment>
						 *	在上面的 XSD 文件中描述了一个新的 targetNamespace ，并在这个空间中定义了一个 name
						 * 为 user的 element，user 有三个属性 id,userName,和 email ,其中 email 的类型为 string，这3个类主要验证
						 * Spring 配置文件中的自定义格式，xsd 文件是 XML DTD 的替代者，使用 XML Schema 语言进行编写，这里对 XSD
						 *
						 *
						 * Schema 不做太多的解析，有兴趣的读者可以参考相关资料
						 * 创建一个文件 ：实现 BeanDefinitionParser 接口，用来解析 XSD 文件中的定义和组件定义
						 *
						 * import org.springframework.beans.factory.support.BeanDefinitionBuilder;
						 * import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
						 * import org.springframework.util.StringUtils;
						 * import org.w3c.dom.Element;
						 * public class UserBeanDefinitionParser extends AbstractSingleBeanDefinitionParser{
						 * 	//Element 对应的类
						 * 	protected Class getBeanClass(Elment element){
						 * 		return User.class;
						 * 	}
						 * 	// 从 element 中解析提取对应的元素
						 * 	protected void doParse(Element element,BeanDefinitionBuilder bean){
						 * 		String userName = element.getAttribute("userName");
						 * 		String email = element.getAttribute("email");
						 * 		// 将提取的数据放到 BeanDefinitionBuilder中，待完成完成所有的 Bean的解析后，统一的注册到 BeanFactory 中
						 * 		if(StringUtils.hasText(userName)){
						 * 				bean.addPropertyValue("userName",userName);
						 * 		}
						 * 		if(StringUtils.hasText(email)){
						 * 			bean.addPropertyValue("email",email);
						 * 		}
						 * }
						 *
						 *  创建一个 Handler文件，扩展自 NamespaceHandlerSupport，目的是将组件注册到 Spring 容器
						 *  import org.Springframework.beans.factory.xml.NamespaceHandlerSupport;
						 *  public class MyNamespaceHandler extends NamespaceHandlerSupport{
						 *  	public void init(){
						 *  		registerBeanDefinitionParser("user",new UserBeanDefinitionParser());
						 *  	}
						 *  }
						 *
						 *  这个代码很简单，无非是当遇到自定义的标签<user:aaa 这样的类似于 user 开头的元素，就会把这个元素扔给对应的
						 *  UserBeanDefinitionParser 去解析
						 *  编写 Spring.handlers 和 Spring.schemas 文件，默认位置是在工程的/META-INF/ 文件夹下，当然，你可以通过
						 *  Spring 的扩展参数或者修改源码的方式来改变路径
						 *  Spring.handlers
						 *  http://www.lexueba.com/schema/user=test.customtag.MyNamespaceHandler
						 *  Spring.schemas
						 *  http://www.lexueba.com/schema/user.xsd=META-INF/Spring-test.xsd
						 *  到这里，自定义的配置就结束了，而 Spring 加载自定义的大致流程就是自定义标签然后就是 Spring.handlers
						 *  和 Spring.schemas 中找到 handler 以及解析元素的 Parser，从而完成整个自定义的元素的解析，也就是说自定义的
						 *  Spring 中默认的标准配置不同于 Spring 将自定义的标签解析的工作委托给了用户去实现
						 *  创建测试文件，在配置文件中引入对应的命名空间以及 XSD 后，便可以直接使用自定义的标签了
						 * <?xml version="1.0" encoding="UTF-8"?>
						 * <beans xmlns="http://www.springframework.org/schema/beans"
						 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						 *        xmlns:myname="http://www.lexueba.com/schema/user"
						 *        ...
						 *        http://www.lexueba.com/schema/user http://www.lexueba.com/schema/user.xsd">
						 *        <myname:user id="testbean" userName="aaa" email="bbb" ></myname:user>
						 * </beans>
						 * 测试，
						 * public static void main(String [] args){
						 * 	ApplicationContext bf = new ClassPathXmlApplicationContext("/test/custometag/test.xml")
						 * 	User user = (User )bf.getBean("testBean");
						 * 	System.out.println(user.getUserName() + "," + user.getEmail()))
						 *
						 * }
						 *
						 * // 如果不出意外的话，你应该可以直接看到我们期待的结果，控制台上打印出了
						 * aaa,bbbb
						 * 上面的例子中，我们实现了通过自定义标签实现了通过属性的方式将 user 类别 的 bean 赋值，在 spring 中自定义的
						 * 标签非常的有用，例如我们熟悉的事务标签，tx(<tx:annotation-driven>)
						 *
						 *
						 * |
						 * <tx:annotation-driven/>
						 * 而两种方式的就读取差别是非常大的，如果采用的是Spring默认的配置，Spring当然知道该怎样做，但是如果是自定义的，
						 * 那么就需要用户实现一些接口及配置了，对于根节点或者子节点如果是默认的命名空间的话，采用parseDefaultElment方法
						 * 进行解析，否则使用delegate.parseCustomElment方法对自定义的命名空间进行解析，而判断是否是默认的命名空间还是
						 * 自定义的命名空间的还是自定义的命名空间，并与Spring中固定的命名空间http://www.springframework.org/schema/beans
						 * 进行比对，如果一致是默认，否则就认为是自定义的，而对于默认的标签解析我们将会在下一章中进行讨论
						 *
						 */
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
		// 如果节点是<alas>别名元素，进行别名解析
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
	 *
	 *
	 * <beans>
	 *     <import resource="customerContext.xml"></import>
	 *     <import resource="systemContext.xml"></import>
	 *     ....
	 * </beans>
	 *
	 *  上面的代码不难，相信配合注解会很好理解，我们总结一下大致的流程便于读者更加好的梳理，在解析<import>标签时，Spring 进行解析步骤大致如下</import>
	 *  1.获取resource 属性所表示的路径
	 *  2.解析路径中的系统属性，格式如下："${user.dir}"
	 *  3.判定 location 是绝对路径还是相对路径
	 *  4.如果是绝对路径则递归调用 bean 的解析过程，进行另一次解析
	 *  5.如果是相对路径则计算出绝对路径再进行解析
	 *  6.通知监听器，解析完成
	 *
	 *  对于嵌入式的 beans 标签，相信大家使用过或者至少接触过，非常类似于 import 标签所提供的功能，
	 *  <?xml version="1.0" encoding="UTF-8"?>
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
	 *  <bean id="aa" class="test.aa"></bean>
	 *  <beans>
	 *
	 *  </beans>
	 *
	 *  |
	 *  这个代码不难，相信配置注释会很好的理解，我们总结一下大致的流程便于读者更好的梳理，在解析<import标签时，Spring进行解析的步骤大致如下
	 *  1.获取resource属性所表示的路径
	 *  2.解析路径中的系统属性，格式如"${user.dir}"
	 *  3.判断location是绝对路径还是相对路径
	 *  4.如果是绝对路径则递归调用bean的解析过程，进行另一次的解析
	 *  5.如果是相对路径则计算出绝对路径并返回
	 *  6.通知监听器，解析完成
	 *
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// 获取给定的导入元素的location属性 | 获取 resource 属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		LogUtils.info(" importBeanDefinitionResource location  :" + location);
		//如果导入元素的location属性值为空，则没有导入任何资源，直接返回 | 如果不存在resource属性则不做任何处理
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		// 使用系统变量值解析location属性值 |  解析系统属性，格式如："${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<Resource>(4);

		// Discover whether the location is an absolute or relative URI
		// 标识给定导入元素的location属性值是否绝对路径。 |  判断 location 属性是绝对 URI 还是相对 URI
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
		// 给定导入元素的location属性值是绝对路径 |  如果是绝对 URI 则直接根据地址加载对应的配置文件
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
			// 给定导入元素的location属性值是相对路径 | 如果是相对地址，则根据相对地址计算出绝对地址
			try {
				int importCount;
				// 将给定的导入元素的location封装为相对路径资源 | resource 存在多个子实现类，如 VfsResource，FileSystemResource 等
				// 而每个 resource 的 createRelative 方法实现都不一样，所以这里先使用子类的方法尝试解析
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				// 封装的相对路径资源存在
				if (relativeResource.exists()) {
					// 使用资源读入器加载Bean的资源
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				// 封装的相对路径资源不存在
				else {
					// 获取Spring IoC容器资源读入器的基本路径 |  如果解析不成功，则使用默认的解析器 ResourcePatternResolver 进行解析
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
		//解析后进行监听器激活处理
		Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
		// 在解析完成<import>元素之后，发送容器导入其他资源处理完成事件
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 *  解析<alias>别名元素，为Bean向Spring IoC容器注册别名
	 *  3.2 alias 标签解析
	 *  通过上面较长的篇幅我们终于分析完了默认标签中的bean标签的处理，那么我们之前提到过，对配置文件的解析包括对import标签，alias标签
	 *  ,bean标签，beans标签的处理，现在我们己经完成了最重要的也就是最核心的功能，其他的解析步骤也都是围绕第3个解析而进行的，在分析了
	 *  第三个解析步骤后，再回头来看看对alias标签的解析。
	 *  对bean进行定义时，除了使用id属性来指定名称之外，为了提供多个名称，可以使用alias标签来指定，而所有的这些名称都指向了同一个bean
	 *  ，在某些情况下，提供别名非常有用，比如为了让应用的的每个组件都更加容易的对公共的组件进行引用。
	 *  然而，在定义bean时就指定了所有的别名并不是总是恰当的，有时， 我们期望能在当前位置为那些别处定义的bean的引入别名，在XML配置文件中
	 *  ，可用单独的<alias/>元素来完成bean的别名的定义，如配置文件中定义了一个javaBean :
	 *  <bean id="testBean" class="com.test"></bean>
	 *  要给这个JavaBean增加别名，以方便不同的对象来调用，我们就可以直接使用bean标签中的name属性：
	 *  <bean id="testBean" class="com.test"></bean>
	 *  <bean name="testBean1,testBean2"></bean>
	 *  考虑一个是加具体的例子，组件 A 在XML配置文件中定义一个名为componentA的DataSource类型的bean，但是组件B却想在其XML文件中以componentB命名来引用
	 *  bean,而且在主程序MyApp的XML配置文件中，希望以myApp的名字来引用此bean，最后容器加载3个XML文件来生成最终的ApplicationContext
	 *  ，在此情形下，可通过在配置文件中添加下列的alias元素来实现。
	 *  <alias name="componentA" alias="componentB"></alias>
	 *  <alias name="componentB" alias="myApp"></alias>
	 *  这样一来，每个组件及主程序都可以通过唯一的名字来引用一个数据源而互不干扰。
	 *  在之前的章节己经讲过对于bean中的name元素解析，那么我们现在再来深入分析对于alias标签的解析过程。
	 *  |
	 *  可以发现，跟之前的讲过的bean中的alias解析大同小异，都是将别名与beanName组成一对注册至registry中，这里不再哆嗦
	 *
	 *
	 */
	protected void processAliasRegistration(Element ele) {
		//获取<alias>别名元素中的name的属性值 | 获取beanName
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		//获取<alias>别名元素中的alias的属性值 | 获取alias
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
				// 向容器的资源读入器注册别名,注册 alias
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// 在解析完<alias>元素之后，发送容器别名处理完成事件,别名注册后通过监听器相应的处理
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 * 解析Bean资源文档对象的普通元素
	 * 通过上述的Spring 容器对载入的Bean定义文档解析可以看出,Spring配置文件中可以使用<import>元素来导入IoC容器所需要的其他资源，
	 * Spring Ioc容器在解析时首先将指定的资源加载到容器中，使用<alias>别名时，Spring Ioc容器首先将别名元素所定义的别名注册到容器中，
	 * 对于既不是<import>元素又不是<alias>的元素，即Spring配置文件中普通的<bean>元素，由BeanDefinitionParserDelegate类的
	 * ParseBeanDefinitionElement()方法实现解析，这个解析过程非常的复杂
	 * |
	 * 1.首先委托BeanDefinitionDelegate类的parseBeanDefinitionElement方法进行元素解析，返回BeanDefinitionHolder类型的bdHolder
	 *   ,经过这个方法后bdHolder实例己经包含我们配置文件中配置的各个属性了，例如class,name,id,alias之类的属性
	 * 2.当返回的bdHolder不为空的情况下若存在默认标签的子节点下再有自定义属性，还需要再次对自定义的属性进行标签解析
	 * 3.解析完成后，需要对解析后的bdHolder进行注册，更新，注册操作委托给了BeanDefinitionReaderUtils的registerBeanDefinition方法
	 * 4.最后发出响应事件，通知相关的监听器，这个bean己经加载好了
	 *
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
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
			// | 通过 getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder)); 来完成此工作，这里实现
			// 只为扩展，当程序开发人员需要对注册的BeanDefinition事件进行监听时可以通过注册监听器方式将处理的逻辑写入监听器中，目前在Spring
			// 并没有对此事做任何逻辑处理
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
