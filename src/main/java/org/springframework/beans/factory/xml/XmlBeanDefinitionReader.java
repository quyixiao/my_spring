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

package org.springframework.beans.factory.xml;

import com.test.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Bean definition reader for XML bean definitions.
 * Delegates the actual XML document reading to an implementation
 * of the {@link BeanDefinitionDocumentReader} interface.
 *
 * <p>Typically applied to a
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * or a {@link org.springframework.context.support.GenericApplicationContext}.
 *
 * <p>This class loads a DOM document and applies the BeanDefinitionDocumentReader to it.
 * The document reader will register each bean definition with the given bean factory,
 * talking to the latter's implementation of the
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} interface.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @see #setDocumentReaderClass
 * @see BeanDefinitionDocumentReader
 * @see DefaultBeanDefinitionDocumentReader
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @since 26.11.2003
 * Xml 配置文件的读取是Spring中重要的功能，因为Spring的大部分功能都是以配置作为切入点的，那么我们可以从XmlBeanDefinitionReader中
 * 梳理一下资源文件的读取，解析及注册的大致脉络，首先我们看看和个类的功能
 *
 * |
 *
 * 通过继承自AbstractBeanDefinitionReader中的方法，来使用ResourceLoader将资源文件路径转换为对应的Resource文件
 *
 */
@Slf4j
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    /**
     * Indicates that the validation should be disabled.
     */
    public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

    /**
     * Indicates that the validation mode should be detected automatically.
     */
    public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

    /**
     * Indicates that DTD validation should be used.
     */
    public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

    /**
     * Indicates that XSD validation should be used.
     */
    public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;


    /**
     * Constants instance for this class
     */
    private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

    private int validationMode = VALIDATION_AUTO;

    private boolean namespaceAware = false;

    private Class<?> documentReaderClass = DefaultBeanDefinitionDocumentReader.class;

    private ProblemReporter problemReporter = new FailFastProblemReporter();

    private ReaderEventListener eventListener = new EmptyReaderEventListener();

    private SourceExtractor sourceExtractor = new NullSourceExtractor();

    private NamespaceHandlerResolver namespaceHandlerResolver;

    private DocumentLoader documentLoader = new DefaultDocumentLoader();

    private EntityResolver entityResolver;

    private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

    private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

    private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
            new NamedThreadLocal<Set<EncodedResource>>("XML bean definition resources currently being loaded");


    /**
     * Create new XmlBeanDefinitionReader for the given bean factory.
     *
     * @param registry the BeanFactory to load bean definitions into,
     *                 in the form of a BeanDefinitionRegistry
     */
    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }


    /**
     * Set whether to use XML validation. Default is {@code true}.
     * <p>This method switches namespace awareness on if validation is turned off,
     * in order to still process schema namespaces properly in such a scenario.
     *
     * @see #setValidationMode
     * @see #setNamespaceAware
     */
    public void setValidating(boolean validating) {
        this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
        this.namespaceAware = !validating;
    }

    /**
     * Set the validation mode to use by name. Defaults to {@link #VALIDATION_AUTO}.
     *
     * @see #setValidationMode
     */
    public void setValidationModeName(String validationModeName) {
        setValidationMode(constants.asNumber(validationModeName).intValue());
    }

    /**
     * Set the validation mode to use. Defaults to {@link #VALIDATION_AUTO}.
     * <p>Note that this only activates or deactivates validation itself.
     * If you are switching validation off for schema files, you might need to
     * activate schema namespace support explicitly: see {@link #setNamespaceAware}.
     */
    public void setValidationMode(int validationMode) {
        this.validationMode = validationMode;
    }

    /**
     * Return the validation mode to use.
     */
    public int getValidationMode() {
        return this.validationMode;
    }

    /**
     * Set whether or not the XML parser should be XML namespace aware.
     * Default is "false".
     * <p>This is typically not needed when schema validation is active.
     * However, without validation, this has to be switched to "true"
     * in order to properly process schema namespaces.
     */
    public void setNamespaceAware(boolean namespaceAware) {
        this.namespaceAware = namespaceAware;
    }

    /**
     * Return whether or not the XML parser should be XML namespace aware.
     */
    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    /**
     * Specify which {@link org.springframework.beans.factory.parsing.ProblemReporter} to use.
     * <p>The default implementation is {@link org.springframework.beans.factory.parsing.FailFastProblemReporter}
     * which exhibits fail fast behaviour. External tools can provide an alternative implementation
     * that collates errors and warnings for display in the tool UI.
     */
    public void setProblemReporter(ProblemReporter problemReporter) {
        this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
    }

    /**
     * Specify which {@link ReaderEventListener} to use.
     * <p>The default implementation is EmptyReaderEventListener which discards every event notification.
     * External tools can provide an alternative implementation to monitor the components being
     * registered in the BeanFactory.
     */
    public void setEventListener(ReaderEventListener eventListener) {
        this.eventListener = (eventListener != null ? eventListener : new EmptyReaderEventListener());
    }

    /**
     * Specify the {@link SourceExtractor} to use.
     * <p>The default implementation is {@link NullSourceExtractor} which simply returns {@code null}
     * as the source object. This means that - during normal runtime execution -
     * no additional source metadata is attached to the bean configuration metadata.
     */
    public void setSourceExtractor(SourceExtractor sourceExtractor) {
        this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new NullSourceExtractor());
    }

    /**
     * Specify the {@link NamespaceHandlerResolver} to use.
     * <p>If none is specified, a default instance will be created through
     * {@link #createDefaultNamespaceHandlerResolver()}.
     */
    public void setNamespaceHandlerResolver(NamespaceHandlerResolver namespaceHandlerResolver) {
        this.namespaceHandlerResolver = namespaceHandlerResolver;
    }

    /**
     * Specify the {@link DocumentLoader} to use.
     * <p>The default implementation is {@link DefaultDocumentLoader}
     * which loads {@link Document} instances using JAXP.
     */
    public void setDocumentLoader(DocumentLoader documentLoader) {
        this.documentLoader = (documentLoader != null ? documentLoader : new DefaultDocumentLoader());
    }

    /**
     * Set a SAX entity resolver to be used for parsing.
     * <p>By default, {@link ResourceEntityResolver} will be used. Can be overridden
     * for custom entity resolution, for example relative to some specific base path.
     */
    public void setEntityResolver(EntityResolver entityResolver) {

        this.entityResolver = entityResolver;
    }

    /**
     * Return the EntityResolver to use, building a default resolver
     * if none specified.
     */
    protected EntityResolver getEntityResolver() {
        log.info("EntityResolver getEntityResolver");
        if (this.entityResolver == null) {
            log.info(" entityResolver is null");
            // Determine default EntityResolver to use.
            ResourceLoader resourceLoader = getResourceLoader();
            if (resourceLoader != null) {
                log.info("getEntityResolver ResourceEntityResolver");
                this.entityResolver = new ResourceEntityResolver(resourceLoader);
            } else {
                log.info("getEntityResolver DelegatingEntityResolver");
                this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
            }
        }

        return this.entityResolver;
    }

    /**
     * Set an implementation of the {@code org.xml.sax.ErrorHandler}
     * interface for custom handling of XML parsing errors and warnings.
     * <p>If not set, a default SimpleSaxErrorHandler is used that simply
     * logs warnings using the logger instance of the view class,
     * and rethrows errors to discontinue the XML transformation.
     *
     * @see SimpleSaxErrorHandler
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Specify the {@link BeanDefinitionDocumentReader} implementation to use,
     * responsible for the actual reading of the XML bean definition document.
     * <p>The default is {@link DefaultBeanDefinitionDocumentReader}.
     *
     * @param documentReaderClass the desired BeanDefinitionDocumentReader implementation class
     */
    public void setDocumentReaderClass(Class<?> documentReaderClass) {
        if (documentReaderClass == null || !BeanDefinitionDocumentReader.class.isAssignableFrom(documentReaderClass)) {
            throw new IllegalArgumentException(
                    "documentReaderClass must be an implementation of the BeanDefinitionDocumentReader interface");
        }
        this.documentReaderClass = documentReaderClass;
    }


    /**
     * Load bean definitions from the specified XML file.
     *
     * @param resource the resource descriptor for the XML file
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     *                                      在XmlBeanDefinitionReader 的抽象父类AbstractBeanDefinitionReader中定义了载入过程，
     *                                      AbstractBeanDefinitionReader的loadBeanDefinitions()如下源码
     *                                      重载方法，调用下面的loadBeanDefinitions(String,Set<Resources>) 方法
     *                                      XmlBeanDefinitionReader加载资源入口方法
     */
    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        log.info(" loadBeanDefinitions resource:" + resource);
        // 将读入的XML资源进行特殊的编码处理
        return loadBeanDefinitions(new EncodedResource(resource));
    }

    /**
     * Load bean definitions from the specified XML file.
     *
     * @param encodedResource the resource descriptor for the XML file,
     *                        allowing to specify an encoding to use for parsing the file
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     *                                      这里载入的XML形式Bean配置信息方法
     *   | 我们再次来整理一下数据准备阶段的逻辑，首先对传入的resource参数做封装，目的就是考虑到Resource可能存在编码要求的情况，其次
     *   SAX读取XML文件的方式来准备InputSource对象，最后将准备的数据通过参数传入真正的核心处理部分，doLoadBeanDefinition(inputResource,encodedResource.getResource())
     */
    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
        log.info("loadBeanDefinitions ");
        Assert.notNull(encodedResource, "EncodedResource must not be null");
        if (logger.isInfoEnabled()) {
            log.info("Loading XML bean definitions from " + encodedResource.getResource());
        }
        // 通过属性来记录己经加载的资源
        Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
        if (currentResources == null) {
            currentResources = new HashSet<EncodedResource>(4);
            log.info(" resourcesCurrentlyBeingLoaded set  currentResources");
            this.resourcesCurrentlyBeingLoaded.set(currentResources);
        }

        if (!currentResources.add(encodedResource)) {
            throw new BeanDefinitionStoreException(
                    "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
        }
        log.info("currentResources  size = " + currentResources.size());
        for (EncodedResource resource : currentResources) {
            log.info(" EncodedResource: " + resource);
        }
        try {
            // 将资源文件转为InputStream的I/O流 |
            // 从encodedResource中获取己经封装的Resource 对象并再次从Resource中获取InputStream
            InputStream inputStream = encodedResource.getResource().getInputStream();
            try {
                // 从InputStream中得到XML的解析源
                // InputSource这个类并不是来自到Spring,而是来自到org.xml.sax
                InputSource inputSource = new InputSource(inputStream);
                if (encodedResource.getEncoding() != null) {
                    inputSource.setEncoding(encodedResource.getEncoding());
                }
                // 这里是具体的读取过程
                return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
            } finally {
                // 关闭输出流
                inputStream.close();
            }
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "IOException parsing XML document from " + encodedResource.getResource(), ex);
        } finally {
            currentResources.remove(encodedResource);
            if (currentResources.isEmpty()) {
                this.resourcesCurrentlyBeingLoaded.remove();
            }
        }
    }

    /**
     * Load bean definitions from the specified XML file.
     *
     * @param inputSource the SAX InputSource to read from
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     * 下面总结一下IoC 容器初始化的基本步骤
     * 1.初始化的入口由容器实现中的refresh()方法来调用完成
     * 2.对bean定义载入的IOC容器使用的方法是loadBeanDefinition()
     * 大致过程如下：通过ResourceLoader来完成资源文件的定位，DefaultResourceLoader是默认的实现，同时上下文本身就给出了ResourceLoader的实现
     * 可以通过类路径，文件系统，URL等方式来定位资源，如果是XMLBeanFactory作为IoC容器，那么需要它指定Bean定义资源，也就是说，Bean定义文件时通过
     * 抽象成Resource来被IoC容器处理，容器通过BeanDefinitionReader来完成定义信息的解析和Bean信息的注册，往往使用XmlBeanDefinitionReader
     * 来解析Bean的xml定义文件，实际处理往往使用BeanDefinitinoParserDelegate来完成，从而得到Bean的定义信息，这些信息在Spring中使用了
     * BeanDefinition来表示，这些名字可以让我们想到的是loadBeanDefinition(),registerBeanDefinition()这些相关的方法，它们都是为了处理
     * BeanDefinition脑子短路的，容器解析得到的BeanDefinition以后，需要在IoC容器中注册，这些由IoC实现的BeanDefinitionRegistry接口实现
     * 注册过程就是在IoC容器内部维护着一个HashMap来保存得到的BeanDefinition的过程，这个HashMap是Ioc容器持有的Bean信息场所，以后对Bean的操作
     * 都是在这个HashMap中实现的
     *
     */
    public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
    }

    /**
     * Load bean definitions from the specified XML file.
     *
     * @param inputSource         the SAX InputSource to read from
     * @param resourceDescription a description of the resource
     *                            (can be {@code null} or empty)
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     */
    public int loadBeanDefinitions(InputSource inputSource, String resourceDescription)
            throws BeanDefinitionStoreException {

        return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
    }


    /**
     * Actually load bean definitions from the specified XML file.
     *
     * @param inputSource the SAX InputSource to read from
     * @param resource    the resource descriptor for the XML file
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     * @see #doLoadDocument
     * @see #registerBeanDefinitions
     * 从特定的XML文件中实际载入Bean资源的配置方法
     */
    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
            throws BeanDefinitionStoreException {
        try {

            LogUtils.info("doLoadBeanDefinitions  ...", 8);
            // 将XML文件转换成DOM对象，解析过程由documentLoader()方法实现
            Document doc = doLoadDocument(inputSource, resource);
            // 根据Document注册Bean信息
            return registerBeanDefinitions(doc, resource);
        } catch (BeanDefinitionStoreException ex) {
            throw ex;
        } catch (SAXParseException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                    "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
        } catch (SAXException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                    "XML document from " + resource + " is invalid", ex);
        } catch (ParserConfigurationException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "Parser configuration exception parsing XML from " + resource, ex);
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "IOException parsing XML document from " + resource, ex);
        } catch (Throwable ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "Unexpected exception parsing XML document from " + resource, ex);
        }
    }

    /**
     * Actually load the specified document using the configured DocumentLoader.
     *
     * @param inputSource the SAX InputSource to read from
     * @param resource    the resource descriptor for the XML file
     * @return the DOM Document
     * @throws Exception when thrown from the DocumentLoader
     * @see #setDocumentLoader
     * @see DocumentLoader#loadDocument
     * 在loadDocument方法中有一个参数EntityResolver，何为EntityResolver，官网这样解析的，如果Sax应用程序需要在实现自定义处理外部实体
     * 则必需实现此接口，并使用setEntityResolver方法向Sax驱动器注册一个实例，也就是说，对于解析一个XML，Sax首先读取该XML文档上声明，根据声明去
     * 寻找相应的DTD定义，以便对文档进行一个验证，默认的寻找规则，即通过网络（实现上就是声明的DTD的URI地址）来下载相应的DTD声明，
     * 并进行认证，下载的过程是一个漫长的过程，而且网络中断或者不可用时，这里会报错，就是因为相应的DTD声明没有被找到的原因
     * EntityResolver的作用的项目本身就是可以提供一个如何寻找DTD声明的方法，即由程序来实现寻找DTD声明的过程，比如我们将DTD文件放到
     * 项目中某处，在实现时直接将此文档读取并返回给SAX即可，这样就避免了网络来寻找相应的声明
     */
    protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
        EntityResolver entityResolver = getEntityResolver();
        boolean isNamespaceAware = isNamespaceAware();
        log.info("entityResolver simpleName : " + entityResolver.getClass().getName() + " isNamespaceAware : " + isNamespaceAware);
        // 加载XML文件，并得到Document
        return this.documentLoader.loadDocument(inputSource,
                entityResolver,
                this.errorHandler,
                // 获取XML文件的验证模式
                getValidationModeForResource(resource), //返回是是DTD 文档还是XSD文档
                isNamespaceAware
        );
    }


    /**
     * Gets the validation mode for the specified {@link Resource}. If no explicit
     * validation mode has been configured then the validation mode is
     * {@link #detectValidationMode detected}.
     * <p>Override this method if you would like full control over the validation
     * mode, even when something other than {@link #VALIDATION_AUTO} was set.
     * DTD和XSD区别
     * DTD（Document Type Definition）即文档类型定义，是一种XML约束模式语言，是XML文件的验证机制，属于XML文件组成的一部分，DTD是一种
     * 保证XML文档格式正确的有效方法，可以通过比较XML文档和DTD文档的包含，元素的定义规则，元素之间关系的定义规则，元素可使用的属性，可合理运用和实体
     * 或符号规则
     * 要使用DTD难模式的时候需要在XML文件的头部声明，以下是在Spring中使用DTD声明方式的代码
     * XML Schema语言就是XSD(XML Schemas Definition)，XML Schema描述了XML文档的结构，可以用一个指定的XML Schema来验证某个XML文档
     * 以检查该XML文档是否符合其要求，文档设计都可以通过XML Schemar指定XML Schema本身的XML文档，它符合XML语法结构，可以通过XML解析器解析它
     * 在使用XML Schema 文档对XML 实例文档进行检验，除了要声明空间外（xmls=http:www.Springframework.og/schema/beans） 还必需指定该名称空间
     * 所对应的Xml Schema文档存储的位置，通过schemaLocation属性来指定名称空间所对应的XML Schema 文档存储位置，它包含两个部分，一部分是名称空间
     * 的URL ，另一部分是该名称空间所标识的XML Schema文件位置或者URL地址（xsi:schemaLocation="http://www.springframework.org/schema/bean
     * http://www.Springframework.org/schema/beans/Spring-beans.xsd"）
     *
     *
     * |
     *
     * 方法的实现其实还是很简单，无非是如果设定了验证模式则使用设定的验证模式（可以通过对调用XmlBeanDefinitionReader中的setValidationMode方法进行设定）
     * 否则使用自动检测的试方式，而自动检测难模式的功能是在函数的detectValidationMode方法中实现的，在detectValidationMode函数中又将自动检测难模式的工作委托给
     * 专门的处理类XmlValidationModeDetector,调用XmlValidationModeDetector方法，具体代码如下：
     *
     *
     */
    protected int getValidationModeForResource(Resource resource) {
        int validationModeToUse = getValidationMode();
        // 如果手动指定了验证模式则使用指定的验证模式
        if (validationModeToUse != VALIDATION_AUTO) {
            log.info(" getValidationModeForResource validationModeToUse != VALIDATION_AUTO ");
            return validationModeToUse;
        }
        // 如果未指定则使用自动检测
        int detectedMode = detectValidationMode(resource);
        log.info(" getValidationModeForResource detectedMode : {},VALIDATION_AUTO :{} ", detectedMode, VALIDATION_AUTO);
        if (detectedMode != VALIDATION_AUTO) {
            log.info(" getValidationModeForResource detectedMode != VALIDATION_AUTO  return detectedMode value is {}", detectedMode);
            return detectedMode;
        }
        // Hmm, we didn't get a clear indication... Let's assume XSD,
        // since apparently no DTD declaration has been found up until
        // detection stopped (before finding the document's root tag).
        log.info(" getValidationModeForResource VALIDATION_XSD value is {} ", VALIDATION_XSD);
        return VALIDATION_XSD;
    }

    /**
     * Detects which kind of validation to perform on the XML file identified
     * by the supplied {@link Resource}. If the file has a {@code DOCTYPE}
     * definition then DTD validation is used otherwise XSD validation is assumed.
     * <p>Override this method if you would like to customize resolution
     * of the {@link #VALIDATION_AUTO} mode.
     */
    protected int detectValidationMode(Resource resource) {
        LogUtils.info(" detectValidationMode get resource ");
        if (resource.isOpen()) {
            throw new BeanDefinitionStoreException(
                    "Passed-in Resource [" + resource + "] contains an open stream: " +
                            "cannot determine validation mode automatically. Either pass in a Resource " +
                            "that is able to create fresh streams, or explicitly specify the validationMode " +
                            "on your XmlBeanDefinitionReader instance.");
        }

        InputStream inputStream;
        try {
            inputStream = resource.getInputStream();
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "Unable to determine validation mode for [" + resource + "]: cannot open InputStream. " +
                            "Did you attempt to load directly from a SAX InputSource without specifying the " +
                            "validationMode on your XmlBeanDefinitionReader instance?", ex);
        }

        try {
            return this.validationModeDetector.detectValidationMode(inputStream);
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("Unable to determine validation mode for [" +
                    resource + "]: an error occurred whilst reading from the InputStream.", ex);
        }
    }

    /**
     * Register the bean definitions contained in the given DOM document.
     * Called by {@code loadBeanDefinitions}.
     * <p>Creates a new instance of the parser class and invokes
     * {@code registerBeanDefinitions} on it.
     *
     * @param doc      the DOM document
     * @param resource the resource descriptor (for context information)
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of parsing errors
     * @see #loadBeanDefinitions
     * @see #setDocumentReaderClass
     * @see BeanDefinitionDocumentReader#registerBeanDefinitions
     * 按照Spring的Bean的语义要求将Bean的配置信息解析并转换为容器内部的数据结构
	 * 首先，通过调用XML解析器将Bean配置信息转换为文档对象，但是这些文档对象并没有按照Spring的Bean规则进行解析，这一步是载入过程
	 * 其次，在完成通过XML解析之后，按照Spring Bean的定义规则对文档对象进行解析，其解析过程在接口BeanDefinitionDocumentReader的实现类
	 * 实现
     *
     *
     * |
     *
     * 其中的参数doc是通过上一节的loadDocument加载转换出来的，在这个方法中很好的应用了面向对象中的单一职责原则，将逻辑处理委托给单一
     * 的类进行处理，而这个逻辑处理类就是BeanDefinitionDocumentReader，BeanDefinitionDocumentReader是一个接口，而实例化的工作就是在
     * createBeanDefinitionDocumentReader()中完成的，而通过此方法，BeanDefinitionDocumentReader 真正的类型就是DefaultBeanDefinitionDocumentReader
     * 了，发现这个方法的重要目的就是提取root ,以便于再次将root作为参数继续BeanDefinition的注册
     *
     */
    public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
    	// 得到BeanDefinitionDocumentReader来对XML格式的BeanDefinition进行解析
        BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
        // 获取容器中的注册的Bean的数量
        int countBefore = getRegistry().getBeanDefinitionCount();
        // 解析过程的入口，这里使用了委派模式，BeanDefinitionDocumentReader只是一个接口
		// 具体的解析过程实现类DefaultBeanDefinitionDocumentReader来完成 | 加载并注册bean
        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
        // 统计解析的Bean的数量
        return getRegistry().getBeanDefinitionCount() - countBefore;
    }

    /**
     * Create the {@link BeanDefinitionDocumentReader} to use for actually
     * reading bean definitions from an XML document.
     * <p>The default implementation instantiates the specified "documentReaderClass".
     *
     * @see #setDocumentReaderClass
     */
    protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
        return BeanDefinitionDocumentReader.class.cast(BeanUtils.instantiateClass(this.documentReaderClass));
    }

    /**
     * Create the {@link XmlReaderContext} to pass over to the document reader.
     */
    public XmlReaderContext createReaderContext(Resource resource) {
        return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
                this.sourceExtractor, this, getNamespaceHandlerResolver());

    }

    /**
     * Lazily create a default NamespaceHandlerResolver, if not set before.
     *
     * @see #createDefaultNamespaceHandlerResolver()
     */
    public NamespaceHandlerResolver getNamespaceHandlerResolver() {
        if (this.namespaceHandlerResolver == null) {
            this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
        }
        return this.namespaceHandlerResolver;
    }

    /**
     * Create the default implementation of {@link NamespaceHandlerResolver} used if none is specified.
     * Default implementation returns an instance of {@link DefaultNamespaceHandlerResolver}.
     */
    protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
        return new DefaultNamespaceHandlerResolver(getResourceLoader().getClassLoader());
    }

}
