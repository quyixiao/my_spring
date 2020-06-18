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

import com.test.LogUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanMetadataAttribute;
import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.util.*;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * Stateful delegate class used to parse XML bean definitions.
 * Intended for use by both the main parser and any extension
 * {@link BeanDefinitionParser BeanDefinitionParsers} or
 * {@link BeanDefinitionDecorator BeanDefinitionDecorators}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Gary Russell
 * @see ParserContext
 * @see DefaultBeanDefinitionDocumentReader
 * @since 2.0
 * 定义解析Element的各种方法
 *
 *
 */
public class BeanDefinitionParserDelegate {

    public static final String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";

    public static final String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; ";

    /**
     * Value of a T/F attribute that represents true.
     * Anything else represents false. Case seNsItive.
     */
    public static final String TRUE_VALUE = "true";

    public static final String FALSE_VALUE = "false";

    public static final String DEFAULT_VALUE = "default";

    public static final String DESCRIPTION_ELEMENT = "description";

    public static final String AUTOWIRE_NO_VALUE = "no";

    public static final String AUTOWIRE_BY_NAME_VALUE = "byName";

    public static final String AUTOWIRE_BY_TYPE_VALUE = "byType";

    public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";

    public static final String AUTOWIRE_AUTODETECT_VALUE = "autodetect";

    public static final String DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE = "all";

    public static final String DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE = "simple";

    public static final String DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE = "objects";

    public static final String NAME_ATTRIBUTE = "name";

    public static final String BEAN_ELEMENT = "bean";

    public static final String META_ELEMENT = "meta";

    public static final String ID_ATTRIBUTE = "id";

    public static final String PARENT_ATTRIBUTE = "parent";

    public static final String CLASS_ATTRIBUTE = "class";

    public static final String ABSTRACT_ATTRIBUTE = "abstract";

    public static final String SCOPE_ATTRIBUTE = "scope";

    private static final String SINGLETON_ATTRIBUTE = "singleton";

    public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

    public static final String AUTOWIRE_ATTRIBUTE = "autowire";

    public static final String AUTOWIRE_CANDIDATE_ATTRIBUTE = "autowire-candidate";

    public static final String PRIMARY_ATTRIBUTE = "primary";

    public static final String DEPENDENCY_CHECK_ATTRIBUTE = "dependency-check";

    public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";

    public static final String INIT_METHOD_ATTRIBUTE = "init-method";

    public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";

    public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";

    public static final String FACTORY_BEAN_ATTRIBUTE = "factory-bean";

    public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";

    public static final String INDEX_ATTRIBUTE = "index";

    public static final String TYPE_ATTRIBUTE = "type";

    public static final String VALUE_TYPE_ATTRIBUTE = "value-type";

    public static final String KEY_TYPE_ATTRIBUTE = "key-type";

    public static final String PROPERTY_ELEMENT = "property";

    public static final String REF_ATTRIBUTE = "ref";

    public static final String VALUE_ATTRIBUTE = "value";

    public static final String LOOKUP_METHOD_ELEMENT = "lookup-method";

    public static final String REPLACED_METHOD_ELEMENT = "replaced-method";

    public static final String REPLACER_ATTRIBUTE = "replacer";

    public static final String ARG_TYPE_ELEMENT = "arg-type";

    public static final String ARG_TYPE_MATCH_ATTRIBUTE = "match";

    public static final String REF_ELEMENT = "ref";

    public static final String IDREF_ELEMENT = "idref";

    public static final String BEAN_REF_ATTRIBUTE = "bean";

    public static final String LOCAL_REF_ATTRIBUTE = "local";

    public static final String PARENT_REF_ATTRIBUTE = "parent";

    public static final String VALUE_ELEMENT = "value";

    public static final String NULL_ELEMENT = "null";

    public static final String ARRAY_ELEMENT = "array";

    public static final String LIST_ELEMENT = "list";

    public static final String SET_ELEMENT = "set";

    public static final String MAP_ELEMENT = "map";

    public static final String ENTRY_ELEMENT = "entry";

    public static final String KEY_ELEMENT = "key";

    public static final String KEY_ATTRIBUTE = "key";

    public static final String KEY_REF_ATTRIBUTE = "key-ref";

    public static final String VALUE_REF_ATTRIBUTE = "value-ref";

    public static final String PROPS_ELEMENT = "props";

    public static final String PROP_ELEMENT = "prop";

    public static final String MERGE_ATTRIBUTE = "merge";

    public static final String QUALIFIER_ELEMENT = "qualifier";

    public static final String QUALIFIER_ATTRIBUTE_ELEMENT = "attribute";

    public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";

    public static final String DEFAULT_MERGE_ATTRIBUTE = "default-merge";

    public static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";

    public static final String DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE = "default-dependency-check";

    public static final String DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE = "default-autowire-candidates";

    public static final String DEFAULT_INIT_METHOD_ATTRIBUTE = "default-init-method";

    public static final String DEFAULT_DESTROY_METHOD_ATTRIBUTE = "default-destroy-method";


    protected final Log logger = LogFactory.getLog(getClass());

    private final XmlReaderContext readerContext;

    private final DocumentDefaultsDefinition defaults = new DocumentDefaultsDefinition();

    private final ParseState parseState = new ParseState();

    /**
     * Stores all used bean names so we can enforce uniqueness on a per
     * beans-element basis. Duplicate bean ids/names may not exist within the
     * same level of beans element nesting, but may be duplicated across levels.
     */
    private final Set<String> usedNames = new HashSet<String>();


    /**
     * Create a new BeanDefinitionParserDelegate associated with the supplied
     * {@link XmlReaderContext}.
     */
    public BeanDefinitionParserDelegate(XmlReaderContext readerContext) {
        Assert.notNull(readerContext, "XmlReaderContext must not be null");
        this.readerContext = readerContext;
    }


    /**
     * Get the {@link XmlReaderContext} associated with this helper instance.
     */
    public final XmlReaderContext getReaderContext() {
        return this.readerContext;
    }

    /**
     * Get the {@link Environment} associated with this helper instance.
     *
     * @deprecated in favor of {@link XmlReaderContext#getEnvironment()}
     */
    @Deprecated
    public final Environment getEnvironment() {
        return this.readerContext.getEnvironment();
    }

    /**
     * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor} to pull the
     * source metadata from the supplied {@link Element}.
     */
    protected Object extractSource(Element ele) {
        return this.readerContext.extractSource(ele);
    }

    /**
     * Report an error with the given message for the given source element.
     */
    protected void error(String message, Node source) {
        this.readerContext.error(message, source, this.parseState.snapshot());
    }

    /**
     * Report an error with the given message for the given source element.
     */
    protected void error(String message, Element source) {
        this.readerContext.error(message, source, this.parseState.snapshot());
    }

    /**
     * Report an error with the given message for the given source element.
     */
    protected void error(String message, Element source, Throwable cause) {
        this.readerContext.error(message, source, this.parseState.snapshot(), cause);
    }


    /**
     * Initialize the default settings assuming a {@code null} parent delegate.
     */
    public void initDefaults(Element root) {
        initDefaults(root, null);
    }

    /**
     * Initialize the default lazy-init, autowire, dependency check settings,
     * init-method, destroy-method and merge settings. Support nested 'beans'
     * element use cases by falling back to the given parent in case the
     * defaults are not explicitly set locally.
     *
     * @see #populateDefaults(DocumentDefaultsDefinition, DocumentDefaultsDefinition, org.w3c.dom.Element)
     * @see #getDefaults()
     */
    public void initDefaults(Element root, BeanDefinitionParserDelegate parent) {
        populateDefaults(this.defaults, (parent != null ? parent.defaults : null), root);
        this.readerContext.fireDefaultsRegistered(this.defaults);
    }

    /**
     * Populate the given DocumentDefaultsDefinition instance with the default lazy-init,
     * autowire, dependency check settings, init-method, destroy-method and merge settings.
     * Support nested 'beans' element use cases by falling back to <literal>parentDefaults</literal>
     * in case the defaults are not explicitly set locally.
     *
     * @param defaults       the defaults to populate
     * @param parentDefaults the parent BeanDefinitionParserDelegate (if any) defaults to fall back to
     * @param root           the root element of the current bean definition document (or nested beans element)
     */
    protected void populateDefaults(DocumentDefaultsDefinition defaults, DocumentDefaultsDefinition parentDefaults, Element root) {
        String lazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
        if (DEFAULT_VALUE.equals(lazyInit)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
            lazyInit = (parentDefaults != null ? parentDefaults.getLazyInit() : FALSE_VALUE);
        }
        defaults.setLazyInit(lazyInit);
        LogUtils.info("populateDefaults lazyInit: " + lazyInit);

        String merge = root.getAttribute(DEFAULT_MERGE_ATTRIBUTE);
        if (DEFAULT_VALUE.equals(merge)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
            merge = (parentDefaults != null ? parentDefaults.getMerge() : FALSE_VALUE);
        }
        LogUtils.info("populateDefaults merge: " + merge);
        defaults.setMerge(merge);

        String autowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
        if (DEFAULT_VALUE.equals(autowire)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to 'no'.
            autowire = (parentDefaults != null ? parentDefaults.getAutowire() : AUTOWIRE_NO_VALUE);
        }
        LogUtils.info("populateDefaults autowire: " + autowire);
        defaults.setAutowire(autowire);

        // Don't fall back to parentDefaults for dependency-check as it's no longer supported in
        // <beans> as of 3.0. Therefore, no nested <beans> would ever need to fall back to it.
        defaults.setDependencyCheck(root.getAttribute(DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE));

        if (root.hasAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE)) {
            defaults.setAutowireCandidates(root.getAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE));
        } else if (parentDefaults != null) {
            defaults.setAutowireCandidates(parentDefaults.getAutowireCandidates());
        }

        if (root.hasAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE)) {
            defaults.setInitMethod(root.getAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE));
        } else if (parentDefaults != null) {
            defaults.setInitMethod(parentDefaults.getInitMethod());
        }

        if (root.hasAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE)) {
            defaults.setDestroyMethod(root.getAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE));
        } else if (parentDefaults != null) {
            defaults.setDestroyMethod(parentDefaults.getDestroyMethod());
        }

        defaults.setSource(this.readerContext.extractSource(root));
    }

    /**
     * Return the defaults definition object, or {@code null} if the
     * defaults have been initialized yet.
     */
    public DocumentDefaultsDefinition getDefaults() {
        return this.defaults;
    }

    /**
     * Return the default settings for bean definitions as indicated within
     * the attributes of the top-level {@code &lt;beans/&gt;} element.
     */
    public BeanDefinitionDefaults getBeanDefinitionDefaults() {
        BeanDefinitionDefaults bdd = new BeanDefinitionDefaults();
        bdd.setLazyInit("TRUE".equalsIgnoreCase(this.defaults.getLazyInit()));
        bdd.setDependencyCheck(this.getDependencyCheck(DEFAULT_VALUE));
        bdd.setAutowireMode(this.getAutowireMode(DEFAULT_VALUE));
        bdd.setInitMethodName(this.defaults.getInitMethod());
        bdd.setDestroyMethodName(this.defaults.getDestroyMethod());
        return bdd;
    }

    /**
     * Return any patterns provided in the 'default-autowire-candidates'
     * attribute of the top-level {@code &lt;beans/&gt;} element.
     */
    public String[] getAutowireCandidatePatterns() {
        String candidatePattern = this.defaults.getAutowireCandidates();
        return (candidatePattern != null ? StringUtils.commaDelimitedListToStringArray(candidatePattern) : null);
    }


    /**
     * Parses the supplied {@code &lt;bean&gt;} element. May return {@code null}
     * if there were errors during parse. Errors are reported to the
     * {@link org.springframework.beans.factory.parsing.ProblemReporter}.
     * 解析bean元素的入口
     */
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
        return parseBeanDefinitionElement(ele, null);
    }

    /**
     * Parses the supplied {@code &lt;bean&gt;} element. May return {@code null}
     * if there were errors during parse. Errors are reported to the
     * {@link org.springframework.beans.factory.parsing.ProblemReporter}.
     * 解析Bean配置信息中的<bean>元素，这个方法中主要处理的<bean>元素的id,name,别名属性
     */
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, BeanDefinition containingBean) {
        // 获取<bean>元素中的id属性值
        String id = ele.getAttribute(ID_ATTRIBUTE);
        // 获取<bean>元素的name属性值
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
        LogUtils.info("parseBeanDefinitionElement id " + id + " nameAttr " + nameAttr);
        // 获取bean元素的alias属性值 | 分割name属性
        List<String> aliases = new ArrayList<String>();
        // 将<bean>元素的中的所有alias属性值放入到别名中
        if (StringUtils.hasLength(nameAttr)) {
            String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
            aliases.addAll(Arrays.asList(nameArr));
        }
        String beanName = id;
        // 如果<bean>元素中没有配置id属性，将别名中的第一个值赋值给beanName
        if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
            beanName = aliases.remove(0);
            if (logger.isDebugEnabled()) {
                logger.debug("No XML 'id' specified - using '" + beanName +
                        "' as bean name and " + aliases + " as aliases");
            }
        }
        // 检查<bean>元素所配置的id或者name唯一性
        if (containingBean == null) {
            //检查<bean>元素的所配置的id,name或者别名是否重复
            checkNameUniqueness(beanName, aliases, ele);
        }
        // 详细对<bean>元素中的配置的bean定义进行解析
        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
        if (beanDefinition != null) {
            LogUtils.info("parseBeanDefinitionElement beanDefinition is null ,beanName : " + beanName);
            if (!StringUtils.hasText(beanName)) {
                try {
                    if (containingBean != null) {
                        // 如果<bean>元素中没有配置id,别名或者name，且没有包含子元素
                        // <bean>元素，则解析的Bean生成一个唯一的beanName并注册
                        beanName = BeanDefinitionReaderUtils.generateBeanName(
                                beanDefinition, this.readerContext.getRegistry(), true);
                        LogUtils.info("parseBeanDefinitionElement containingBean is not null ,beanName : " + beanName);
                    } else {
                        // 如果<bean>元素没有配置id,别名或者name，且包含子元素
                        // <bean>元素，则将解析的Bean生成一个唯一的BeanName并注册
                        beanName = this.readerContext.generateBeanName(beanDefinition);

                        LogUtils.info("parseBeanDefinitionElement containingBean is null ,beanName : " + beanName);
                        // Register an alias for the plain bean class name, if still possible,
                        // if the generator returned the class name plus a suffix.
                        // This is expected for Spring 1.2/2.0 backwards compatibility.
                        String beanClassName = beanDefinition.getBeanClassName();
                        // 为解析的Bean使用别名注册时，为了向后兼容
                        // Spring 1.2 /2.0 给别名添加后缀
                        if (beanClassName != null &&
                                beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
                                !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                            aliases.add(beanClassName);
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Neither XML 'id' nor 'name' specified - " +
                                "using generated bean name [" + beanName + "]");
                    }
                } catch (Exception ex) {
                    error(ex.getMessage(), ele);
                    return null;
                }
            }
            String[] aliasesArray = StringUtils.toStringArray(aliases);
            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
        }
        // 当解析出错时，返回null
        return null;
    }

    /**
     * Validate that the specified bean name and aliases have not been used already
     * within the current level of beans element nesting.
     */
    protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
        String foundName = null;

        if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
            foundName = beanName;
        }
        if (foundName == null) {
            foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
        }
        if (foundName != null) {
            error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
        }

        this.usedNames.add(beanName);
        this.usedNames.addAll(aliases);
    }

    /**
     * Parse the bean definition itself, without regard to name or aliases. May return
     * {@code null} if problems occurred during the parsing of the bean definition.
     * 详细对<bean>元素中配置的Bean定义的其他的属性进行解析
     * 由于上面的方法已经对Bean的id,name和别名属性进行了处理
     * 该方法主要是处理除了这三个以外的其他的属性
     * 【注意】在解析<bean>元素的过程中没有创建和实例化<bean>对象，只是创建了Bean元素的定义类BeanDefinition,将<bean>元素中的信息
     * 设置到了BeanDefinition中作为记录，当依赖注入时才使用这些记录信息创建和实例化具体的Bean对象
     * 在对一些配置(如meta,qualifier等)的解析，我们在Spring中使用得不多，在使用Spring<Bean>元素时，配置最多的就是<property>子元素
     */
    public AbstractBeanDefinition parseBeanDefinitionElement(
            Element ele, String beanName, BeanDefinition containingBean) {
        LogUtils.all("parseBeanDefinitionElement beanName " + beanName);
        // 记录解析<bean>元素
        this.parseState.push(new BeanEntry(beanName));
        // 这里只读取<bean>元素中配置的class名字，然后载入BeanDefinition中
        // 只是记录配置的class名字，不做实例化，对象的实例化在依赖注入的时候完成
        String className = null;
        if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
            className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
        }
        LogUtils.info("parseBeanDefinitionElement className :" + className);
        try {
            String parent = null;
            // 如果<bean>元素中配置了parent属性，则获取 parent属性的值
            if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
                parent = ele.getAttribute(PARENT_ATTRIBUTE);
            }
            // 根据<bean>元素配置的class名称和parent属性值创建BeanDefinition
            // 为载入的Bean定义信息做准备
            AbstractBeanDefinition bd = createBeanDefinition(className, parent);
            // 对当前的<bean>元素中配置的一些属性进行解析和设置，如果配置了单态（singleton）属性等
            parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
            // 为<bean>元素解析的Bean设备描述信息
            bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
            // 为<bean>元素的meta(元信息进行解析)
            parseMetaElements(ele, bd);
            // 为<bean>元素的lookup-Method属性进行解析
            parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
            // 为<bean>元素的replaced-Method属性进行解析
            parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
            //解析<bean>元素构造方法设置
            parseConstructorArgElements(ele, bd);
            //解析<bean>元素的<property>设置
            parsePropertyElements(ele, bd);
            // 解析<bean>元素的qualifier属性
            parseQualifierElements(ele, bd);
            // 为当前的解析了Bean设置所需要的资源和依赖对象
            bd.setResource(this.readerContext.getResource());
            bd.setSource(extractSource(ele));
            return bd;
        } catch (ClassNotFoundException ex) {
            error("Bean class [" + className + "] not found", ele, ex);
        } catch (NoClassDefFoundError err) {
            error("Class that bean class [" + className + "] depends on not found", ele, err);
        } catch (Throwable ex) {
            error("Unexpected failure during bean definition parsing", ele, ex);
        } finally {
            this.parseState.pop();
        }
        // 当前解析<bean>元素出错时，返回null
        return null;
    }

    /**
     * Apply the attributes of the given bean element to the given bean * definition.
     *
     * @param ele            bean declaration element
     * @param beanName       bean name
     * @param containingBean containing bean definition
     * @return a bean definition initialized according to the bean element attributes
     */
    public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
                                                                BeanDefinition containingBean, AbstractBeanDefinition bd) {

        // 解析scope 属性
        if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
            error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
        } else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
            bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
        } else if (containingBean != null) {
            LogUtils.info("parseBeanDefinitionAttributes scope :" + containingBean.getScope());
            // Take default from containing bean in case of an inner bean definition.
            // 在嵌入beanDefinition的情况下且没有单独指定scope的属性则使用父类默认的
            bd.setScope(containingBean.getScope());
        }
        //解析abstract属性
        if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
            bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
        }

        // 解析lazy-init属性
        String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
        if (DEFAULT_VALUE.equals(lazyInit)) {
            lazyInit = this.defaults.getLazyInit();
        }
        //若没有设置或者设置其他的字符会被设置为false
        bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
        LogUtils.info("parseBeanDefinitionAttributes lazyInit :" + lazyInit + " , bd lazyInit :" + bd.isLazyInit());
        //解析autowire属性
        String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
        bd.setAutowireMode(getAutowireMode(autowire));
        LogUtils.info("parseBeanDefinitionAttributes AutowireMode :" + bd.getAutowireMode());
        // 解析dependency-check属性
        String dependencyCheck = ele.getAttribute(DEPENDENCY_CHECK_ATTRIBUTE);
        bd.setDependencyCheck(getDependencyCheck(dependencyCheck));
        //解析depends-on属性
        if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
            bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
        }
        //解析autowire-candidate属性
        String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
        LogUtils.info("parseBeanDefinitionAttributes autowireCandidate :" + autowireCandidate);
        if ("".equals(autowireCandidate) || DEFAULT_VALUE.equals(autowireCandidate)) {
            String candidatePattern = this.defaults.getAutowireCandidates();
            LogUtils.info("parseBeanDefinitionAttributes candidatePattern :" + candidatePattern);
            if (candidatePattern != null) {
                String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
                bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
            }
        } else {
            bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
        }
        LogUtils.info("parseBeanDefinitionAttributes autowireCandidate value :" + bd.isAutowireCandidate());
        //解析primary属性
        if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
            bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
        }
        //解析init-method属性
        if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
            String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
            if (!"".equals(initMethodName)) {
                bd.setInitMethodName(initMethodName);
            }
        } else {
            if (this.defaults.getInitMethod() != null) {
                bd.setInitMethodName(this.defaults.getInitMethod());
                bd.setEnforceInitMethod(false);
            }
        }
        //解析destroy-method属性
        if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
            String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
            bd.setDestroyMethodName(destroyMethodName);
        } else {
            if (this.defaults.getDestroyMethod() != null) {
                bd.setDestroyMethodName(this.defaults.getDestroyMethod());
                bd.setEnforceDestroyMethod(false);
            }
        }
        // 解析factory-method属性
        if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
            bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
        }
        // 解析factory-bean属性
        if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
            bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
        }

        return bd;
    }

    /**
     * Create a bean definition for the given class name and parent name.
     *
     * @param className  the name of the bean class
     * @param parentName the name of the bean's parent bean
     * @return the newly created bean definition
     * @throws ClassNotFoundException if bean class resolution was attempted but failed
     */
    protected AbstractBeanDefinition createBeanDefinition(String className, String parentName)
            throws ClassNotFoundException {

        return BeanDefinitionReaderUtils.createBeanDefinition(
                parentName, className, this.readerContext.getBeanClassLoader());
    }

    /**
     * 3.解析子元素meta
     * 在开始解析元素之前，我们先回顾一下元数据meta属性的使用
     * <bean id="myTestBean" class="bean.MyTestBean">
     *      <meta key="testStr" value="aaaaaaa"/>
     * </bean>
     * 这代码并不会体现MyTestBean的属性当中，而是一个额外的声明，当需要使用里面的时候，可以通过BeanDefinition的getAttribute(key)方法
     * 获取
     */
    public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
        // 获取当前节点的所有的子元素
        NodeList nl = ele.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            // 提取meta
            if (isCandidateElement(node) && nodeNameEquals(node, META_ELEMENT)) {
                Element metaElement = (Element) node;
                String key = metaElement.getAttribute(KEY_ATTRIBUTE);
                String value = metaElement.getAttribute(VALUE_ATTRIBUTE);
                //使用key,value 构造BeanMetadataAttribute
                BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
                attribute.setSource(extractSource(metaElement));
                // 记录信息
                attributeAccessor.addMetadataAttribute(attribute);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public int getAutowireMode(String attValue) {

        String att = attValue;
        if (DEFAULT_VALUE.equals(att)) {
            att = this.defaults.getAutowire();
        }

        int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
        if (AUTOWIRE_BY_NAME_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
        } else if (AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
        } else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
        } else if (AUTOWIRE_AUTODETECT_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_AUTODETECT;
        }
        // Else leave default value.
        LogUtils.info("getAutowireMode attValue :" + attValue + " ,att = " + att + " ,autowire =" + autowire);
        return autowire;
    }

    public int getDependencyCheck(String attValue) {
        String att = attValue;
        if (DEFAULT_VALUE.equals(att)) {
            att = this.defaults.getDependencyCheck();
        }
        if (DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE.equals(att)) {
            return AbstractBeanDefinition.DEPENDENCY_CHECK_ALL;
        } else if (DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE.equals(att)) {
            return AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS;
        } else if (DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE.equals(att)) {
            return AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE;
        } else {
            return AbstractBeanDefinition.DEPENDENCY_CHECK_NONE;
        }
    }

    /**
     * Parse constructor-arg sub-elements of the given bean element.
     * 6.解析子元素constructor-arg
     *  对构造函数是是非常常用的，同时也是非常复杂的，也相信大家对构造函数的配置都不阳生 ，举个简单的例子来说
     *  ...
     *  <beans>
     *      <!--默认的情况下是按照参数的顺序注入的，当指定的index索引后就可以改变了-->
     *      <bean id="helloBean" class="com.HelloBean">
     *          <constructor-arg index = "0">
     *              <value>郝佳</value>
     *          </constructor-arg>
     *          <constructor-arg index="1">
     *              <value>你好</value>
     *         </constructor-arg>
     *      </bean>
     *  </beans>
     *  上面的配置是Spring构造函数配置中最佳的基础配置，实现功能就是对HelloBean自动寻找对的函数，并在初始化的时候将设置参数传入进去
     *  ，那么让我们来看具体的XML解析过程
     */
    public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
                parseConstructorArgElement((Element) node, bd);
            }
        }
    }

    /**
     * Parse property sub-elements of the given bean element.
     * 解析<bean>元素中的<property>元素
     * 7.解析子元素property
     * parsePropertyElement函数完成了对property属性的提取，property使用方式如下
     * <bean id="test" class="test.TestClass">
     *      <property name="testStr" value="aaa"></property>
     * </bean>
     * 或者
     * <bean id="a">
     *      <property name="p">
     *             <list>
     *                 <value> aaa</value>
     *                 <value> bb </value>
     *             </list>
     *      </property>
     * </bean>
     *
     */
    public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
        // 获取<bean>元素中的所有的子元素
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            // 如果子元素中<property>元素的子元素，则调用解析<property>子元素的方法解析
            if (isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)) {
                parsePropertyElement((Element) node, bd);
            }
        }
    }

    /**
     * Parse qualifier sub-elements of the given bean element.
     */
    public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ELEMENT)) {
                parseQualifierElement((Element) node, bd);
            }
        }
    }

    /**
     * Parse lookup-override sub-elements of the given bean element.
     * 4.解析子元素lookup-method
     * 同样，子元素lookup-method似乎并不是很常用，但是某些时候它的确是非常有用的属性，通常我们称它为获取器注入，引用Spring in Action
     * 中的一一句话：获取器注入是一种特殊的方法注入，它是把一个方法声明为返回某个类型的bean ,但实际要返回的bean是在配置文件里配置的，此方法
     * 可用在设置有些可插拔的功能上，解除程序的依赖，我们看看具体的应用
     * (1)首先我我们创建一个类
     * package test.lookup.bean ;
     * public class User {
     *   public void showMe(){
     *         System.out.println("i am user");
     *     }
     * }
     * (2)创建其子类覆盖showMe方法
     * package test.lookup.bean ;
     * public class Teacher extends User{
     *     public void showMe(){
     *         System.out.println("I am Teacher");
     *     }
     * }
     * (3)创建调用方法
     * public abstract class GetBeanTest{
     *     public void showMe(){
     *         this.getBean().showMe();
     *     }
     *     public abstract User getBean();
     * }
     * (4)创建测试方法
     * package test.lookup;
     * import org.springframework.context.ApplicationContext;
     * import org.springframework.context.support.ClassPathXmlApplicationContext;
     * import test.lookup.app.GetBeanTest;
     * public class Main{
     *     ApplicationContext bf = new ClassPathApplicationContext("test/lookup/lookupTest.xml");
     *     GetBeanTest test = (GetBeanTest)bf.getBean("getBeanTest");
     *     test.showMe();
     * }
     * 到目前为止，除了配置文件外，整个测试的方法就己经完成，如果之前没有接触过获取器注入的读者可能会有疑问：抽象方法还没有被实现，怎么就可以
     * 直接被调用了呢？答案就是Spring为我们提供的获取器中，我们看看配置文件是怎样配置的呢？
     * <?xml version="1.0" encoding="UTF-8"?>
     * <beans xmlns="http://www.springframework.org/schema/beans"
     *      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *      xsi:schemaLocation="http://www.springframework.org/schema/beans
     *      http://org.springframework.org/schema/beans/spring-beans.xsd">
     *      <bean id="getBeanTest" class="test.lookup.app.GetBeanTest">
     *          <lookup-method name="getBean" bean="teacher"></lookup-method>
     *      </bean>
     *      <bean id="teacher" class="test.lookup.bean.Teacher"></bean>
     *
     * </beans>
     *
     * 的这个配置文件中，我们看到源码解析提到lookup-method子元素，这个配置完成的功能是动态的将teacher所代表的bean作为getBean的返回值
     * 运行测试方法我们会看到控制台上的输出
     * i am Teacher
     * 当我们的业务变更或者其他的情况下，teacher里面的业务逻辑己经不再符合我们的业务要求了，需要进行替换怎样办呢？ 这里我们需要增加新的逻辑
     * 类
     * package test.lookup.bean ;
     * public class Student extends User{
     *     public void showMe(){
     *         System.out.println("i am student ");
     *     }
     * }
     *同样修改配置文件
     * <beans xmlns="http://www.springframework.org/schema/beans"
     *      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *      xsi:schemaLocation="http://www.springframework.org/schema/beans
     *      http://org.springframework.org/schema/beans/spring-beans.xsd">
     *      <bean id="getBeanTest" class="test.lookup.app.GetBeanTest">
     *          <lookup-method name="getBean" bean="student"></lookup-method>
     *      </bean>
     *       <bean id="student" class="test.lookup.bean.Student"></bean>
     * </beans>
     * 打印出 i am Student
     * 至此，我们己经初步的了解了lookup-method子元素所提供的大致功能，相信这个时候属性的提取源码会觉得更加有针对性
     *
     * |
     * 这个方法似乎也parseMetaElements的代码大同小异，最大的区别就是if判断中的节点名称在这里被修改为LOOKUP_METHOD_ELEMENT，还有
     * ，在数据存储上面通过使用LookupOverride类型实体类来进行数据承载并记录在AbstractBeanDefinition中的methodOverrides属性中
     *
     */
    public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            // 仅当前的Spring默认的bean子元素下且为 lookup-method时有效
            if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
                Element ele = (Element) node;
                // 获取要修饰的方法
                String methodName = ele.getAttribute(NAME_ATTRIBUTE);
                // 获取要配置的bean
                String beanRef = ele.getAttribute(BEAN_ELEMENT);
                LookupOverride override = new LookupOverride(methodName, beanRef);
                override.setSource(extractSource(ele));
                overrides.addOverride(override);
            }
        }
    }

    /**
     * Parse replaced-method sub-elements of the given bean element.
     * 5.解析子元素replace-method
     * 这个方法的主要是对bean中的replaced-method子元素的提取，在开始提取分析之前我们还是预先介绍下这个元素的用法
     * 方法替换，可以在运行时用新的方法替换现有的方法，与之前的look-up不同的是，replaced-method不但可以替换返回实体的bean ,而且还能动态
     * 的更改原有的逻辑，我们看看使用示例
     * (1)在changeMe中完成某个业务逻辑
     * public class TestChangeMethod {
     *     public void changeMe(){
     *         System.out.println("change me");
     *     }
     * }
     * (2)在运营一段时间后需要改变原有的业务逻辑
     * public class TestMethodReplacer implements MethodReplacer{
     *     System.out.println("我替换了原有的方法");
     *     return null;
     * }
     * (3)使替换后的类生效
     * <beans xmlns="http://www.springframework.org/schema/beans"
     *      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *      xsi:schemaLocation="http://www.springframework.org/schema/beans
     *      http://org.springframework.org/schema/beans/spring-beans.xsd">
     *      <bean id="testChangeMethod" class="test.replacedmethod.TestChange">
     *          <replaced-method name="changeMe" replacer="replacer"></replaced-method>
     *      </bean>
     *      <bean id="replacer" class="test.replacemethod.TestMethodReplacer"></bean>
     *   </beans>
     * (4)测试
     * public static void main(String [] args){
     *     ApplicationContext bf = new ClassPathApplicationContext("test/replacemethod/replaceMethod.xml");
     *     TestChangeMethod test = (TestChangeMethod)bf.getBean("testChangeMethod");
     *     test.changeMe();
     * }
     * 打印：
     * 我替换了原有的方法
     * 也就是说，我们动态的改变了原来的方法，知道了这个元素的用法，我们再来看看提取的过程
     *
     * 我们可以看到，无论是look-up还是replace-method都是构造了一个MethodOverride，并最终记录在了AbstractBeanDefinition中的methodOverrides属性中
     * ,而这个属性如何使用以完成它所提供的功能呢，这个要在后续的章节中进行详细的介绍
     */
    public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            // 仅当在Spring默认的bean的子元素下且为<replace-method时有效
            if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
                Element replacedMethodEle = (Element) node;
                // 提取要替换的旧的方法
                String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
                // 提取对应的新的替换方法
                String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
                ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
                // Look for arg-type match elements.
                List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
                for (Element argTypeEle : argTypeEles) {
                    // 记录参数
                    String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE);
                    match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
                    if (StringUtils.hasText(match)) {
                        replaceOverride.addTypeIdentifier(match);
                    }
                }
                replaceOverride.setSource(extractSource(replacedMethodEle));
                overrides.addOverride(replaceOverride);
            }
        }
    }

    /**
     * Parse a constructor-arg element.
     * 这个代码看起来复杂，但是涉及的逻辑其实并不复杂，首先是提取constructor-arg上必要的属性（index,type,name）
     * 如果配置中指定的index属性，那么操作步骤如下
     * 1.解析Constructor-arg的子元素
     * 2.使用ConstructorArgumentValues.ValueHolder类型来封装解析出来的元素
     * 3.将type,name和index属性一并封装在ConstructorArgumentValues.ValueHolder类型中并添加至当前的BeanDefinition的ConstructorArgumentValues
     * 的indexedArgumentValues属性中
     *
     * 如果没有指定index属性，那么操作步骤如下：
     * 1.解析constructor-arg的子元素
     * 2.使用ConstructorArgumentValues.ValueHolder类型来封装解析出来的元素
     * 3.将type,name和index属性一并封装在ConstructorArgumentValues.ValueHolder类型中并添加至当前的BeanDefinition的ConstructorArgumentValues
     * 的genericArgumentValues属性中
     * 可以看到，对于是否制定的index属性来讲，Spring处理流程是不同的，关键在于属性信息被保存的位置
     * 那么整个流程后，我们尝试着进一步了解解析构造函数配置中子元素的过程，进入parsePropertyValue:
     *
     */
    public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
        // 提取index 属性
        String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
        // 提取type属性
        String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
        // 提取name属性
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
        if (StringUtils.hasLength(indexAttr)) {
            try {
                int index = Integer.parseInt(indexAttr);
                if (index < 0) {
                    error("'index' cannot be lower than 0", ele);
                } else {
                    try {
                        this.parseState.push(new ConstructorArgumentEntry(index));
                        // 解析ele对应的属性元素
                        Object value = parsePropertyValue(ele, bd, null);
                        ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                        if (StringUtils.hasLength(typeAttr)) {
                            valueHolder.setType(typeAttr);
                        }
                        if (StringUtils.hasLength(nameAttr)) {
                            valueHolder.setName(nameAttr);
                        }
                        valueHolder.setSource(extractSource(ele));
                        // 不允许重复指定相同的参数
                        if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
                            error("Ambiguous constructor-arg entries for index " + index, ele);
                        } else {
                            bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
                        }
                    } finally {
                        this.parseState.pop();
                    }
                }
            } catch (NumberFormatException ex) {
                error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
            }
        } else {
            // 如果没有index属性则忽略去属性，自动寻找
            try {
                this.parseState.push(new ConstructorArgumentEntry());
                Object value = parsePropertyValue(ele, bd, null);
                ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                if (StringUtils.hasLength(typeAttr)) {
                    valueHolder.setType(typeAttr);
                }
                if (StringUtils.hasLength(nameAttr)) {
                    valueHolder.setName(nameAttr);
                }
                valueHolder.setSource(extractSource(ele));
                bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
            } finally {
                this.parseState.pop();
            }
        }
    }

    /**
     * Parse a property element.
     * 解析<property>元素
     * |
     *
     * 可以看到上面的函数注入方式不同的是将返回值使用PropertyValue进行封装，并记录在BeanDefinition中的propertyValue属性中
     */
    public void parsePropertyElement(Element ele, BeanDefinition bd) {
        // 获取<property>元素的名字| 获取子元素中的name值
        String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
        if (!StringUtils.hasLength(propertyName)) {
            error("Tag 'property' must have a 'name' attribute", ele);
            return;
        }
        this.parseState.push(new PropertyEntry(propertyName));
        try {
            // 如果一个Bean中已经存在同名的<property>元素存在，则不进行解析，直接返回
            // 即如果存在同一个Bean中配置同名的<property>元素，则只有第一个起作用
            // | 不允许多次对同一属性配置
            if (bd.getPropertyValues().contains(propertyName)) {
                error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
                return;
            }
            //解析获取的<property>元素的值
            Object val = parsePropertyValue(ele, bd, propertyName);
            //根据<property>元素的名字和值创建实例
            PropertyValue pv = new PropertyValue(propertyName, val);
            //解析<property>元素中的meta属性
            parseMetaElements(ele, pv);
            pv.setSource(extractSource(ele));
            bd.getPropertyValues().addPropertyValue(pv);
        } finally {
            this.parseState.pop();
        }
    }

    /**
     * Parse a qualifier element.
     * 8.解析子元素qualifier
     * 对于qualifier元素的获取，我们接触更多的是注解的形式，在使用Spring框架中进行自动注入时，Spring容器中匹配的候选Bean数目必需有且只有一个
     * 当找不到匹配的bean时，Spring容器将抛出BeanCreationException异常，并指出必需至少拥有一个匹配的Bean
     * Spring允许我们通过Qualifier指定的注入Bean的名称，这样歧义就消除了，而对于配置方式使用如下：
     * <bean id="myTestBean" class="bean.MyTestBean">
     *      <qualifier type="org.springframework.beans.factory.annotation.Qualifier" value="xxx"></qualifier>
     * </bean>
     */
    public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
        String typeName = ele.getAttribute(TYPE_ATTRIBUTE);
        if (!StringUtils.hasLength(typeName)) {
            error("Tag 'qualifier' must have a 'type' attribute", ele);
            return;
        }
        this.parseState.push(new QualifierEntry(typeName));
        try {
            AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
            qualifier.setSource(extractSource(ele));
            String value = ele.getAttribute(VALUE_ATTRIBUTE);
            if (StringUtils.hasLength(value)) {
                qualifier.setAttribute(AutowireCandidateQualifier.VALUE_KEY, value);
            }
            NodeList nl = ele.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ATTRIBUTE_ELEMENT)) {
                    Element attributeEle = (Element) node;
                    String attributeName = attributeEle.getAttribute(KEY_ATTRIBUTE);
                    String attributeValue = attributeEle.getAttribute(VALUE_ATTRIBUTE);
                    if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
                        BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
                        attribute.setSource(extractSource(attributeEle));
                        qualifier.addMetadataAttribute(attribute);
                    } else {
                        error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
                        return;
                    }
                }
            }
            bd.addQualifier(qualifier);
        } finally {
            this.parseState.pop();
        }
    }

    /**
     * Get the value of a property element. May be a list etc.
     * Also used for constructor arguments, "propertyName" being null in this case.
     * 解析获取<property>元素的值
     * 通过下面的源码解析，我们了解了Spring配置文件中的<bean>元素中的<property>子元素的相关配置，
     * 1.ref 被封装成指向依赖对象的一个引用
     * 2.value 被封装成一个字符串类型的对象
     * 3.ref 和value都是通过解析数据类型属性值.setSource(extractSource(ele));将方法的属性或者引用与引用属性关系起来
     * 最后<property>元素的子元素通过parsePropertySubElement()方法解析，下面我们继续分析该方法的源码，了解其解析过程
     *
     * |
     *
     *
     * 从代码上来看，对函数的属性元素的解析，经历了以下的几个过程
     * 1.略过description或者meta
     * 2.提取constructor-arg上的ref和value属性，以便于根据规则验证正确性，其规则为在constructor-arg 上不存在以下的情况
     * 同时既有ref又有value属性
     * 存在ref属性或者value属性且又有子元素
     * 3.ref属性的处理，使用RunTimeBeanReference封装对应的ref名称，如：
     * <constructor-arg ref="a">
     *
     * </constructor-arg>
     * 4.value属性的处理，使用TypeStringValue封装，如：
     * <constructor-arg value="a"></constructor-arg>
     * 5.子元素的处理
     * <constructor-arg >
     *      <map>
     *          <entry key="key" value="value"></entry>
     *      </map>
     * </constructor-arg>
     * 而对于子元素的处理，例如，这里反映到的在构造函数中嵌入了子元素map是怎样实现的呢？parsePropertySubElement中对实现了对各种子元素的处理
     */

    public Object parsePropertyValue(Element ele, BeanDefinition bd, String propertyName) {
        String elementName = (propertyName != null) ?
                "<property> element for property '" + propertyName + "'" :
                "<constructor-arg> element";
        // 获取<property>中的所有的子元素，只能是ref,value,list,etc中的一种类型
        // Should only have one child element: ref, value, list, etc.
        NodeList nl = ele.getChildNodes();
        Element subElement = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            // 子元素是description和meta属性 不做处理
            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
                    !nodeNameEquals(node, META_ELEMENT)) {
                // Child element is what we're looking for.
                if (subElement != null) {
                    error(elementName + " must not contain more than one sub-element", ele);
                } else {
                    // 当property元素包含子元素
                    subElement = (Element) node;
                }
            }
        }
        // 判断属性值是ref还是value，不允许既是ref 又是value | 解析constructor-arg 的ref 属性
        boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
        // 解析constructor-arg 上的value属性
        boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
        if ((hasRefAttribute && hasValueAttribute) ||
                ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
            /**
             * 在constructor-arg上不存在：
             * 1.同时既有ref属性又有value属性
             * 2.存在ref属性或者value属性且又有子元素
             */
            error(elementName +
                    " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
        }
        // 如果属性值是ref ，创建一个ref 的数据对象，RuntimeBeanReference，这个对象封装了ref
        if (hasRefAttribute) {
            String refName = ele.getAttribute(REF_ATTRIBUTE);
            if (!StringUtils.hasText(refName)) {

                error(elementName + " contains empty 'ref' attribute", ele);
            }
            //一个指向运行是所依赖对象的引用 | ref属性的处理，使用RuntimeBeanReference封装对应的ref名称
            RuntimeBeanReference ref = new RuntimeBeanReference(refName);
            ref.setSource(extractSource(ele));
            return ref;
            // 如果属性值是value,创建一个value数据对象，typedStringValue，这个对象封装了value
            // | value属性的处理，使用TypedStringValue封装
        } else if (hasValueAttribute) {
            // 一个持有String类型的对象
            TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
            // 设置这个value的数据对象被当前对象所引用
            valueHolder.setSource(extractSource(ele));
            return valueHolder;
        } else if (subElement != null) {
            // 解析<property>子元素
            return parsePropertySubElement(subElement, bd);
        } else {
            // 属性值既不是ref也不是value，解析出错，返回null
            // Neither child element nor "ref" or "value" attribute found.
            error(elementName + " must specify a ref or value", ele);
            return null;
        }
    }

    public Object parsePropertySubElement(Element ele, BeanDefinition bd) {
        return parsePropertySubElement(ele, bd, null);
    }

    /**
     * Parse a value, ref or collection sub-element of a property or
     * constructor-arg element.
     *
     * @param ele              subelement of property element; we don't know which yet
     * @param defaultValueType the default type (class name) for any
     *                         {@code &lt;value&gt;} tag that might be created
     *                         解析<property>元素中的ref,value或者集合等元素
     * 通过下面的源码可以得到，在Spring配置文件中，对<property>元素中的配置的<array>，<list>,<set>，<map>，<props>，等各种子元素都
     *                         通过上面的方法解析，生成对应的数据对象，比如ManagedList,ManagedArray,ManagedSet,等,这些managed类是
     *                         Spring对象的BeanDefinition的数据封装，对集合数据类型的具体解析由各种解析方法实现，解析方法的命名也非常的
     *                         一目了然
     * 经过对Spring Bean配置信息转换文档对象中的元素层层解析，Spring Ioc现在已经将XML 形式定义的Bean配置信息转换为Spring Ioc所识别的数据
     * 结构，BeanDefinition,它是Bean配置信息中的POJO对象在Spring IOC容器中的映射，我们通过AbstractBeanDefinition作为入口，
     *                         看如何在Spring Ioc容器进行索引，查询，和其他的操作
     *                         通过Spring Ioc容器对Bean 的配置信息的解析，    Spring IOc 容器大致完成了Bean 对象的唯一的准备工作
     *                         ，即初始化过程，但是最的重要的依赖注入还没有发生，在Spring Ioc容器中BeanDefinition在存储的还是一些静态的
     *                         信息，接下来，需要向容器注册Bean定义信息，才能真正的完成IOC容器的初始化工作
     *
     *
     *
     */
    public Object parsePropertySubElement(Element ele, BeanDefinition bd, String defaultValueType) {
        LogUtils.all("parsePropertySubElement ");
        // 如果<property>元素没有使用Spring默认的命名空间，则使用用户自定义的规则解析内嵌元素
        if (!isDefaultNamespace(ele)) {
            return parseNestedCustomElement(ele, bd);
            //如果子元素是Bean,则使用解析<bean>元素的方法解析
        } else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
            BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
            if (nestedBd != null) {
                nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
            }
            return nestedBd;
            //如果子元素是ref,ref只能有3个属性，bean,local,parent
        } else if (nodeNameEquals(ele, REF_ELEMENT)) {
            // A generic reference to any name of any bean.
            // 可以不在同一个Spring配置文件中上，具体参考 Spring 对ref配置规则
            String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
            boolean toParent = false;
            if (!StringUtils.hasLength(refName)) {
                // A reference to the id of another bean in the same XML file.
                //解析local
                refName = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
                if (!StringUtils.hasLength(refName)) {
                    // A reference to the id of another bean in a parent context.
                    //  获取<property>元素中的parent属性值，引用父容器中的Bean
                    refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
                    toParent = true;
                    if (!StringUtils.hasLength(refName)) {
                        error("'bean', 'local' or 'parent' is required for <ref> element", ele);
                        return null;
                    }
                }
            }
            if (!StringUtils.hasText(refName)) {
                error("<ref> element contains empty target attribute", ele);
                return null;
            }
            // 创建ref类型数据，指向被引用的对象
            RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
            //设置引用类型值被当前的子元素所引用
            ref.setSource(extractSource(ele));
            return ref;
            //如果子元素是<idref>,使用解析<ref>元素的方法进行解析
        } else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
            return parseIdRefElement(ele);
            //如果子元素是<value>,使用解析<value>元素的方法解析
        } else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
            return parseValueElement(ele, defaultValueType);
            //如果子元素是null，则为<property>元素设置一个封装null值的字符串数据
        } else if (nodeNameEquals(ele, NULL_ELEMENT)) {
            // It's a distinguished null value. Let's wrap it in a TypedStringValue
            // object in order to preserve the source location.
            TypedStringValue nullHolder = new TypedStringValue(null);
            nullHolder.setSource(extractSource(ele));
            return nullHolder;
            //如果子元素是<Array>，则使用解析<array>集合元素的方法进行解析
        } else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
            return parseArrayElement(ele, bd);
            //如果子元素是<list>，则使用解析<list>元素的方法进行解析
        } else if (nodeNameEquals(ele, LIST_ELEMENT)) {
            return parseListElement(ele, bd);
            //如果子元素是<set>，则使用<set>集合子元素的方法解析
        } else if (nodeNameEquals(ele, SET_ELEMENT)) {
            return parseSetElement(ele, bd);
            // 如果子元素是<map>，则使用<map>集合中的元素方法进行解析
        } else if (nodeNameEquals(ele, MAP_ELEMENT)) {
            return parseMapElement(ele, bd);
            //如果子元素是<props>，则使用<props>集合元素中的方法进行解析
        } else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
            return parsePropsElement(ele);
            //如果既不是ref又不是value,也不是集合，则说明子元素配置错误，返回null
        } else {
            error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
            return null;
        }
    }

    /**
     * Return a typed String value Object for the given 'idref' element.
     */
    public Object parseIdRefElement(Element ele) {
        // A generic reference to any name of any bean.
        String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
        if (!StringUtils.hasLength(refName)) {
            // A reference to the id of another bean in the same XML file.
            refName = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
            if (!StringUtils.hasLength(refName)) {
                error("Either 'bean' or 'local' is required for <idref> element", ele);
                return null;
            }
        }
        if (!StringUtils.hasText(refName)) {
            error("<idref> element contains empty target attribute", ele);
            return null;
        }
        RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
        ref.setSource(extractSource(ele));
        return ref;
    }

    /**
     * Return a typed String value Object for the given value element.
     */
    public Object parseValueElement(Element ele, String defaultTypeName) {
        // It's a literal value.
        String value = DomUtils.getTextValue(ele);
        String specifiedTypeName = ele.getAttribute(TYPE_ATTRIBUTE);
        String typeName = specifiedTypeName;
        if (!StringUtils.hasText(typeName)) {
            typeName = defaultTypeName;
        }
        try {
            TypedStringValue typedValue = buildTypedStringValue(value, typeName);
            typedValue.setSource(extractSource(ele));
            typedValue.setSpecifiedTypeName(specifiedTypeName);
            return typedValue;
        } catch (ClassNotFoundException ex) {
            error("Type class [" + typeName + "] not found for <value> element", ele, ex);
            return value;
        }
    }

    /**
     * Build a typed String value Object for the given raw value.
     *
     * @see org.springframework.beans.factory.config.TypedStringValue
     */
    protected TypedStringValue buildTypedStringValue(String value, String targetTypeName)
            throws ClassNotFoundException {

        ClassLoader classLoader = this.readerContext.getBeanClassLoader();
        TypedStringValue typedValue;
        if (!StringUtils.hasText(targetTypeName)) {
            typedValue = new TypedStringValue(value);
        } else if (classLoader != null) {
            Class<?> targetType = ClassUtils.forName(targetTypeName, classLoader);
            typedValue = new TypedStringValue(value, targetType);
        } else {
            typedValue = new TypedStringValue(value, targetTypeName);
        }
        return typedValue;
    }

    /**
     * Parse an array element.
     */
    public Object parseArrayElement(Element arrayEle, BeanDefinition bd) {
        String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
        NodeList nl = arrayEle.getChildNodes();
        ManagedArray target = new ManagedArray(elementType, nl.getLength());
        target.setSource(extractSource(arrayEle));
        target.setElementTypeName(elementType);
        target.setMergeEnabled(parseMergeAttribute(arrayEle));
        parseCollectionElements(nl, target, bd, elementType);
        return target;
    }

    /**
     * Parse a list element.
     * 解析<list>集合元素
     */
    public List<Object> parseListElement(Element collectionEle, BeanDefinition bd) {
        //获取<list>元素中的value-type属性，即获取集合元素中的数据类型
        String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
        //获取<list>元素中的所有的集合结点
        NodeList nl = collectionEle.getChildNodes();
        //Spring中将list封装成ManagedList
        ManagedList<Object> target = new ManagedList<Object>(nl.getLength());
        target.setSource(extractSource(collectionEle));
        //设置集合目标的数据类型
        target.setElementTypeName(defaultElementType);

        target.setMergeEnabled(parseMergeAttribute(collectionEle));
        //具体的<list>元素的解析
        parseCollectionElements(nl, target, bd, defaultElementType);
        return target;
    }

    /**
     * Parse a set element.
     */
    public Set<Object> parseSetElement(Element collectionEle, BeanDefinition bd) {
        String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
        NodeList nl = collectionEle.getChildNodes();
        ManagedSet<Object> target = new ManagedSet<Object>(nl.getLength());
        target.setSource(extractSource(collectionEle));
        target.setElementTypeName(defaultElementType);
        target.setMergeEnabled(parseMergeAttribute(collectionEle));
        parseCollectionElements(nl, target, bd, defaultElementType);
        return target;
    }

    //具体的解析<list>集合子元素，<array>,<list>和<set>都使用了方法解析
    protected void parseCollectionElements(
            NodeList elementNodes, Collection<Object> target, BeanDefinition bd, String defaultElementType) {
        // 遍历集合的所有的了节点
        for (int i = 0; i < elementNodes.getLength(); i++) {
            Node node = elementNodes.item(i);
            //节点不是description节点
            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
                //将解析的元素加入到集合，递归调用下一个元素
                target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
            }
        }
    }

    /**
     * Parse a map element.
     */
    public Map<Object, Object> parseMapElement(Element mapEle, BeanDefinition bd) {
        String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE);
        String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE);

        List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
        ManagedMap<Object, Object> map = new ManagedMap<Object, Object>(entryEles.size());
        map.setSource(extractSource(mapEle));
        map.setKeyTypeName(defaultKeyType);
        map.setValueTypeName(defaultValueType);
        map.setMergeEnabled(parseMergeAttribute(mapEle));

        for (Element entryEle : entryEles) {
            // Should only have one value child element: ref, value, list, etc.
            // Optionally, there might be a key child element.
            NodeList entrySubNodes = entryEle.getChildNodes();
            Element keyEle = null;
            Element valueEle = null;
            for (int j = 0; j < entrySubNodes.getLength(); j++) {
                Node node = entrySubNodes.item(j);
                if (node instanceof Element) {
                    Element candidateEle = (Element) node;
                    if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
                        if (keyEle != null) {
                            error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
                        } else {
                            keyEle = candidateEle;
                        }
                    } else {
                        // Child element is what we're looking for.
                        if (nodeNameEquals(candidateEle, DESCRIPTION_ELEMENT)) {
                            // the element is a <description> -> ignore it
                        } else if (valueEle != null) {
                            error("<entry> element must not contain more than one value sub-element", entryEle);
                        } else {
                            valueEle = candidateEle;
                        }
                    }
                }
            }

            // Extract key from attribute or sub-element.
            Object key = null;
            boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE);
            boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE);
            if ((hasKeyAttribute && hasKeyRefAttribute) ||
                    ((hasKeyAttribute || hasKeyRefAttribute)) && keyEle != null) {
                error("<entry> element is only allowed to contain either " +
                        "a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
            }
            if (hasKeyAttribute) {
                key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
            } else if (hasKeyRefAttribute) {
                String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
                if (!StringUtils.hasText(refName)) {
                    error("<entry> element contains empty 'key-ref' attribute", entryEle);
                }
                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
                ref.setSource(extractSource(entryEle));
                key = ref;
            } else if (keyEle != null) {
                key = parseKeyElement(keyEle, bd, defaultKeyType);
            } else {
                error("<entry> element must specify a key", entryEle);
            }

            // Extract value from attribute or sub-element.
            Object value = null;
            boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE);
            boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE);
            boolean hasValueTypeAttribute = entryEle.hasAttribute(VALUE_TYPE_ATTRIBUTE);
            if ((hasValueAttribute && hasValueRefAttribute) ||
                    ((hasValueAttribute || hasValueRefAttribute)) && valueEle != null) {
                error("<entry> element is only allowed to contain either " +
                        "'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
            }
            if ((hasValueTypeAttribute && hasValueRefAttribute) ||
                    (hasValueTypeAttribute && !hasValueAttribute) ||
                    (hasValueTypeAttribute && valueEle != null)) {
                error("<entry> element is only allowed to contain a 'value-type' " +
                        "attribute when it has a 'value' attribute", entryEle);
            }
            if (hasValueAttribute) {
                String valueType = entryEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
                if (!StringUtils.hasText(valueType)) {
                    valueType = defaultValueType;
                }
                value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), valueType, entryEle);
            } else if (hasValueRefAttribute) {
                String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
                if (!StringUtils.hasText(refName)) {
                    error("<entry> element contains empty 'value-ref' attribute", entryEle);
                }
                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
                ref.setSource(extractSource(entryEle));
                value = ref;
            } else if (valueEle != null) {
                value = parsePropertySubElement(valueEle, bd, defaultValueType);
            } else {
                error("<entry> element must specify a value", entryEle);
            }

            // Add final key and value to the Map.
            map.put(key, value);
        }

        return map;
    }

    /**
     * Build a typed String value Object for the given raw value.
     *
     * @see org.springframework.beans.factory.config.TypedStringValue
     */
    protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
        try {
            TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
            typedValue.setSource(extractSource(entryEle));
            return typedValue;
        } catch (ClassNotFoundException ex) {
            error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle, ex);
            return value;
        }
    }

    /**
     * Parse a key sub-element of a map element.
     */
    protected Object parseKeyElement(Element keyEle, BeanDefinition bd, String defaultKeyTypeName) {
        NodeList nl = keyEle.getChildNodes();
        Element subElement = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                // Child element is what we're looking for.
                if (subElement != null) {
                    error("<key> element must not contain more than one value sub-element", keyEle);
                } else {
                    subElement = (Element) node;
                }
            }
        }
        return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
    }

    /**
     * Parse a props element.
     */
    public Properties parsePropsElement(Element propsEle) {
        ManagedProperties props = new ManagedProperties();
        props.setSource(extractSource(propsEle));
        props.setMergeEnabled(parseMergeAttribute(propsEle));

        List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
        for (Element propEle : propEles) {
            String key = propEle.getAttribute(KEY_ATTRIBUTE);
            // Trim the text value to avoid unwanted whitespace
            // caused by typical XML formatting.
            String value = DomUtils.getTextValue(propEle).trim();
            TypedStringValue keyHolder = new TypedStringValue(key);
            keyHolder.setSource(extractSource(propEle));
            TypedStringValue valueHolder = new TypedStringValue(value);
            valueHolder.setSource(extractSource(propEle));
            props.put(keyHolder, valueHolder);
        }

        return props;
    }

    /**
     * Parse the merge attribute of a collection element, if any.
     */
    public boolean parseMergeAttribute(Element collectionElement) {
        String value = collectionElement.getAttribute(MERGE_ATTRIBUTE);
        if (DEFAULT_VALUE.equals(value)) {
            value = this.defaults.getMerge();
        }
        return TRUE_VALUE.equals(value);
    }

    public BeanDefinition parseCustomElement(Element ele) {
        return parseCustomElement(ele, null);
    }

    //  containingBd 为父类的 BeanDefinition ，对顶层元素的解析应该设置为 null
    //  其实思路非常的简单，无非是根据对应的Bean获取对应的命名空间，根据命名空间解析对应的处理器，然后根据用户自定义的处理器进行解析，
    // 可是有些事情说起来简单做起来难，我们先看看如何获取命名空间吧。
    public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {
        // 获取对应的命名空间
        String namespaceUri = getNamespaceURI(ele);
        LogUtils.info("parseCustomElement namespaceUri :" + namespaceUri);
        // 根据命名空间找到对应的 NamespaceHandler，
        // 在 readerContext 初始化的时候，其属性 namespaceHandlerResolver 已经被初始化成了DefaultNamespaceHandlerResolver
        // 的实例了，所以这里调用 resolver 方法其实调用的是 DefaultNamespaceHandlerResolver 类中的方法，我们进入了
        // DefaultNamespaceHandlerResolver 的 resolver 方法进行查看
        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);

        LogUtils.info("parseCustomElement handler name :" + (handler != null ? handler.getClass().getName() : null));

        if (handler == null) {
            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
            return null;
        }
        // 调用自定义的 NamespaceHandler  进行解析
        // 得到了解析器以及要分析的元素后，Spring 就可以将解析工作委托给自定义的解析器去解析，在 Spring 中的代码为
        //  return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
        // 以前提到的示例进行分析，此时的 Handler 已经被实例化成为我们自定义的 MyNameSpaceHandler 了，而 MyNamespaceHandler
        // 已经完成了初始化的工作，但是我们实现自定义的命名空间处理器并没有实现 parse 方法，所以推断，这个方法是父类中实现的
        return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
    }

    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder definitionHolder) {
        // 这里将函数中的第三个设置为空，那么第三个参数是做什么用的呢？什么情况下不为空呢？其实这第三个参数是父类的bean,当对某个嵌套
        // 配置进行分析时，这里需要传递父类beanDefinition，分析源码得知这里传递的参数其实是为了使用父类的scope属性，以备子类若没有设置
        // scope时默认使用父类的属性，这里分析的是顶层配置，所以传递null,将第三个参数设置为空后进一步跟踪函数：
        return decorateBeanDefinitionIfRequired(ele, definitionHolder, null);
    }
    // 我们总结一下 decorateBeanDefinitionIfRequired 方法的作用，在 decorateBeanDefinitionIfRequired 中，我们可以看到程序的
    // 默认的标签的处理其实是直接略过的，因此，默认的标签到这里已经被处理完成，这里对自定义的标签或者说对 bean 的我自定义属性感兴趣
    // 在方法中实现了寻找自定义标签并根据自定义标签寻找命名空间处理器，并进行进一步的解析
    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(
            Element ele, BeanDefinitionHolder definitionHolder, BeanDefinition containingBd) {
        BeanDefinitionHolder finalDefinition = definitionHolder;

        // Decorate based on custom attributes first.
        NamedNodeMap attributes = ele.getAttributes();

        LogUtils.all("decorateBeanDefinitionIfRequired length :" + attributes.getLength());
        //遍历所有的属性，看看是否有适用于修饰的属性
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);

            LogUtils.info("decorateBeanDefinitionIfRequired node name :" + node.getNodeName());
            finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
        }

        // Decorate based on custom nested elements.
        // 遍历所有的子节点，看看是否有适用于修饰的子元素
        NodeList children = ele.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            LogUtils.info("decorateBeanDefinitionIfRequired children node name :" + node.getNodeName() + " , nodeType = " + node.getNodeType());
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
            }
        }
        return finalDefinition;
    }

    /***
     * 程序直到这里，条理其实己经非常的清楚了，首先获取属性或者元素的命名空间，以此来判断该元素或者属性是否适用于自定义标签的解析条件，
     * 找出自定义类型所对应的NamespaceHandler并进行进一步的解析，在自定义标签解析的章节我们会重点讲解，这里暂先略过
     *
     */
    public BeanDefinitionHolder decorateIfRequired(
            Node node, BeanDefinitionHolder originalDef, BeanDefinition containingBd) {
        // 获取自己定义标签的命名空间
        String namespaceUri = getNamespaceURI(node);
        LogUtils.info("decorateIfRequired namespaceUri :" + namespaceUri);
        // 对于非默认标签进行修饰
        if (!isDefaultNamespace(namespaceUri)) {
            LogUtils.info("decorateIfRequired isDefaultNamespace namespaceUri :" + namespaceUri);
            // 根据命名空间找到对应的处理器
            // 程序走到这里，条理其实已经非常的清楚了，首先获取属性或者元素的命名空间，以此来判断该元素或者属性是否适用于自定义标签的解析条件
            // 找出自定义类型所对应的NamespaceHandler并进行进一步的解析
            NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
            if (handler != null) {
                // 进行修饰
                return handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
            } else if (namespaceUri != null && namespaceUri.startsWith("http://www.springframework.org/")) {
                error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
            } else {
                // A custom namespace, not to be handled by Spring - maybe "xml:...".
                if (logger.isDebugEnabled()) {
                    logger.debug("No Spring NamespaceHandler found for XML schema namespace [" + namespaceUri + "]");
                }
            }
        }
        return originalDef;
    }

    private BeanDefinitionHolder parseNestedCustomElement(Element ele, BeanDefinition containingBd) {
        BeanDefinition innerDefinition = parseCustomElement(ele, containingBd);
        if (innerDefinition == null) {
            error("Incorrect usage of element '" + ele.getNodeName() + "' in a nested manner. " +
                    "This tag cannot be used nested inside <property>.", ele);
            return null;
        }
        String id = ele.getNodeName() + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR +
                ObjectUtils.getIdentityHexString(innerDefinition);
        if (logger.isDebugEnabled()) {
            logger.debug("Using generated bean name [" + id +
                    "] for nested custom element '" + ele.getNodeName() + "'");
        }
        return new BeanDefinitionHolder(innerDefinition, id);
    }


    /**
     * Get the namespace URI for the supplied node. The default implementation uses {@link Node#getNamespaceURI}.
     * Subclasses may override the default implementation to provide a different namespace identification mechanism.
     *
     * @param node the node
     * 标签的解析是从命名空间中提起开始，无论是区分 Spring 默认的标签和还是区分自定义标签中不同的标签的处理器是以标签所提供的命名空间
     *             为基础的，而至于如何提取对应的元素命名空间其实并不需要我们亲自去实现，在 org.w3c.dom.Node 中已经提供了方法，直接
     *              给我们调用
     *
     *
     */
    public String getNamespaceURI(Node node) {
        LogUtils.info("getNamespaceURI node Name :" + node.getClass().getName());
        return node.getNamespaceURI();
    }

    /**
     * Ges the local name for the supplied {@link Node}. The default implementation calls {@link Node#getLocalName}.
     * Subclasses may override the default implementation to provide a different mechanism for getting the local name.
     *
     * @param node the {@code Node}
     */
    public String getLocalName(Node node) {
        return node.getLocalName();
    }

    /**
     * Determine whether the name of the supplied node is equal to the supplied name.
     * <p>The default implementation checks the supplied desired name against both
     * {@link Node#getNodeName()} and {@link Node#getLocalName()}.
     * <p>Subclasses may override the default implementation to provide a different
     * mechanism for comparing node names.
     *
     * @param node        the node to compare
     * @param desiredName the name to check for
     */
    public boolean nodeNameEquals(Node node, String desiredName) {
        return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
    }

    public boolean isDefaultNamespace(String namespaceUri) {
        return (!StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri));
    }

    public boolean isDefaultNamespace(Node node) {
        String uri = getNamespaceURI(node);
        LogUtils.info("isDefaultNamespace uri " + uri);
        return isDefaultNamespace(uri);
    }

    private boolean isCandidateElement(Node node) {
        return (node instanceof Element && (isDefaultNamespace(node) || !isDefaultNamespace(node.getParentNode())));
    }

}
