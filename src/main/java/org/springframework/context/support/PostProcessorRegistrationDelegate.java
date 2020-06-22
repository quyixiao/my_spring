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

package org.springframework.context.support;

import com.test.LogUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
class PostProcessorRegistrationDelegate {

    /***
     * BeanFactory 的后处理
     * BeanFactory 作为 Spring 中容器功能基础，用于存在所有已经加载的 bean ,为了保证程序上的高可扩展性，Spring 针对BeanFactory
     *  做了大量的扩展，比如我们熟知的 PostProcessor 等都是在这里实现的
     *  激活注册 beanFactoryPostProcessor
     *   正式开始介绍之前，我们先了解一下 BeanFactoryPostProcessor 的用法
     *   BeanFactoryPostProcessor 接口跟 BeanPostProcessor 类似，可以对 bean 的定义配置无数进行处理，也就是说，Spring IOC
     *    容器允许 BeanFactoryPostProcessor 在容器实际实例化任何其他的 bean 之前读取配置元数据，并有可能修改它，如果你愿意，
     *    你可以配置多个 BeanFactoryPostProcessor ，你还能通过设置"order" 属性来控制 beanFactoryPostProcessor 的执行次序
     *  仅当 BeanFactoryPostProcessor 实现了 Ordered 接口时你才可以设置此属性，因此 在实现 beanFactoryPostProcessor 时，就
     *  应当考虑实现 Ordered接口。请参考 BeanFactoryPostProcessor 和 Ordered 接口的 JavaDoc 以获取更加详细的信息
     * 如果你想改变实际的 bean 的实例，例如从配置元数据创建的对象，那么你最好使用 BeanPostProcessor ，同样的，BeanFactoryPostProcessor
     * 的作用作用域范围的容器级的，它只和你使用的容器相关，如果你在容器中定义了一个 BeanFactoryPostProcessor，它仅仅对此容器中的 bean
     * 进行后置处理，BeanFactoryPostProcessor 不会对定义在另一个容器上的 bean 进行后置处理，即使这两个容器都是在同一层次上，在 Spring
     * 中存在对 BeanFactoryPostProcessor 的典型应用，比如 PropertyPlaceHolderConfigurer
     * 1.BeanFactoryPostProcessor的典型应用 ：PropertyPlaceholderConfigurer
     *
     *   有个时候，阅读 Spring 的 Bean 描述文件时，你也许会遇到类似的如下的一些配置
     *   <bean id ="message" class="distconfig.HelloMessage">
     *      <property name="msg">
     *             <value>${bean.message}</value>
     *      </property>
     *   </bean>
     *   其中竟然出现了变量引用：${bean.message} 就是 Spring分散配置，可以在另外的配置文件中bean.message 指定值，如在 bean.property 配置如下定义
     *   bean.message=Hi.can you find me?
     *   当访问名为 message 的 bean 时，mes 的属性会被设置为字符串"Hi can you find me ?" 但是 Spring 框架是怎样知道这样的配置文件的呢？
     *   这就要靠 PropertyPlaceholderConfigurer 这个类的 bean :
     *   <bean id="msgHandler" class="org.Springframework.beans.factory.config.Property.PlaceholderConfigurer">
     *      <property name="locations">
     *          <list>
     *              <value>config/bean.properties</value>
     *          </list>
     *      </property>
     *   </bean>
     *   在这个 bean 中指定的配置文件为 config/bean.properties，到这里似乎找到问题的答案了,但是其实还有问题，这个"mesHandler" 只不过
     *   是 Spring 框架管理的一个 bean ,并没有被识别 bean 或者对象引用，Spring 的 beanFactory 是怎样知道要从这个 bean 获取配置信息
     *   的呢？
     *      查看层级结构可以看出 PropertyPlaceHolderConfigure 这个类间接的继承了 BeanFactoryPostProcessor接口，这是一个很特别的接口
     *  当 Spring 加载任何实现了这个接口的 bean 的配置时，都会在 bean 工厂加载所有的 bean 的配置之后执行PostProcessorBeanFactory
     *  方法，在 PropertyResourceConfigurer 类中实现了，postProcessBeanFactory 方法，在方法中先后调用了 mergeProperties，
     *  convertProperties，processProperties 这3个方法，分别得到配置，将得到的配置转换为合适的类型，最后将配置内容告知 BeanFactory
     *      正是通过实现 BeanFactoryPostProcessor 接口，beanFactory会在实例化任何的 bean 之前获得配置信息，从而能够
     *       正确的解析 bean 描述文件中的变量引用
     *  2.使用自定义的 BeanFactoryPostProcessor
     *      我们以实现一个 BeanFactoryPostProcessor ，去除潜在的流氓属性值的功能来展示自定义BeanFactoryPostProcessor 的创建及使用
     *  例如 bean 的定义中留下 bollcokes 这样的字眼
     *  <?xml version="1.0" encoding="UTF-8"?>
     * <beans xmlns="http://www.springframework.org/schema/beans"
     *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
     *        <bean id="bfapp" class="com.spring.ch04.ObscenityRemovingBeanFactoryPostProcessor">
     *          <property name="obscenties">
     *                 <set>
     *                     <value>bollocks</value>
     *                     <value>winky</value>
     *                     <value>bum</value>
     *                     <value>Microsoft</value>
     *                 </set>
     *          </property>
     *          <bean id="simpleBean" class="com.Spring.ch04.SimplePostProcessor">
     *              <property name="connectionString" value="bollocks"> </property>
     *              <property name="password" value="imaginecup"></property>
     *              <property name="username" value="Microsoft"></property>
     *          </bean>
     *        </bean>
     *
     *    public class ObscenityRemovingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
     *        private Set<String> obscenties;
     *        public ObscenityRemovingBeanFactoryPostProcessor(){
     *              this.obscenties = new HashSet();
     *        }
     *        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException{
     *              String []  beanNames = beanFactory.getBeanDefinitionNames();
     *              for(String beanName:beanNames){
     *                  BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
     *                  StringValueResolver valueResover = new StringValueResovler(){
     *                      public String resolveStringValue(String strVal){
     *                          if(isObscene(strVal)){
     *                              return "*****";
     *                          }
     *                          return strVal;
     *                      }
     *                  };
     *                  BeanDefinitionVisitor visitor = new BeanDefintionVisitor(valueResover);
     *                  visitor.visiBeanDefintion(bd);
     *              }
     *        }
     *        public boolean isObscene(Object value){
     *          String potentialObscenity = value.toString().toUpperCase();
     *          return this.obscenties.contains(potentialObscenity);
     *        }
     *
     *        public void setObscenties(Set<String> obscenties){
     *          this.obscenties.clear();
     *          for(String obscenity :obscenties){
     *                  this.obscenties.add(obscenity.toUpperCase());
     *          }
     *        }
     *    }
     *    执行类
     *    public class PropertyConfigurerDemo{
     *        public static void main(String [] args){
     *            ConfigurableListableBeanFactory bf = new XmlBeanFactory(new ClassPathResource("/META-INF/BeanFactory.xml"));
     *            BeanFactoryPostProcessor bfpp = (BeanFactoryPostProcessor)bf.getBean("bfpp");
     *            bfpp.postProcessBeanFactory(bf);
     *            sout(bf.getBean("simpleBean"));
     *        }
     *    }
     *
     *
        * 输出结果
     * SimplePostProcessor{connectionStrin obscenies=*****,username=****,password=imaginecup}
     *
     *  通过 ObscenityRemovingBeanFactoryPostProcessor Spring 很好的实现了屏蔽掉不应该展示的属性
     *
     *   |
     *
     *   从上面的方法中我们可以看到，对于 BeanFactoryPostProcessor 的处理主要分成两种情况进行，一个是对于 BeanDefinitionRegistry
     *   类特殊处理，另一种是对普通的 BeanFactoryPostProcessor 进行处理，而对于每种情况都需要进行考虑硬编码注入注册后的处理器
     *   以及通过配置注入的后处理器
     *
     *   对于硬编码注册的后处理器的处理，主要通过 AbstractApplicationContext 中的添加处理器的方法 addBeanFactoryPostProcessor 进行
     *   添加
     *   public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
     *       this.beanFactoryPostProcessors.add(beanFactoryPostProcessor);
     *   }
     *   添加的后处理器会存在在 beanFactoryPostProcessors 中，而在处理器 BeanFactoryPostProcessor 的时候会首先检测 beanFactoryPostProcessors
     *   不但有 BeanFactoryPostProcessor 的特性。
     *
     *   同时还有自定义的修改化方法，也需要在此调用，所以这里需要从 BeanFactoryPostProcessor 中挑出 BeanDefinitionRegistryPostProcessor 的生处理器，并进行其
     *   postProcessBeanDefinitionRegistry 方法的激活
     *
     *   记录后处理器主要使用三个List 来完成
     *   registryPostProcessors:记录通过硬编码的方式注册的BeanDefinitionRegistryPostProcessor 类型的处理器
     *   regularPostProcessor:记录通过硬编码的方式注册的 BeanFactoryPostProcessor 类开处理
     *
     *   registryPostProcessorBeans :  记录通过配置的方式注册 BeanDefinitionRegistryPostProcessor 类型的处理器
     *   对以上的 List记录的 List中后的处理器进行统一的调用 BeanFactoryPostProcessor 的 postProcessBeanFactory
     *   方法
     *   5.普通的 beanFactory 处理
     *   BeanDefinitionRegistryPostProcessor 只对 BeanDefinitionRegistry 类型 ConfigurableListableBeanFactory 有效，
     *   所以，   如果判断所示的 beanFactory 并不是 BeanDefinitionRegistry ，那么便可以忽略了BeanDefinitionRegistryPostProcesssor
     *   而直接处理 BeanFactoryPostProcessor ，当然获取的方式与上面的获取的方式类似
     *
     *   这里需要提到的是，对于硬编码方式手动添加的手处理是不需要做任何排序的，但是在配置文件中读取的处理器，Spring 并不能保证读取的顺序
     *   ，所以，为了保证用户的调用顺序的要求，Spring 对于后处理器的调用支持按照 PriorityOrdered 或者 Ordered 的顺序调用
     * 6.6.2 注册 BeanPostProcessor
     *  在这个里面我们提到了 BeanFactoryPostProcessors 的调用，现在我们来探索下 BeanPostProcessor ，但是在这里并不是调用，而是注册
     *  真正的调用其实是在 bean 的实例化阶段进行的，这是一个很重要的步骤，也是很多功能 BeanFactory 不支持的重要原因，Spring 中大部分
     *  功能都是通过后处理器的方式进行扩展的，这是spring 框架的一个特性，但是在 BeanFactory 中其实并没有实现后处理自动注册，所以在
     *  调用的时候如果没有进行手动注册其实是不能使用的，但是在 ApplicationContext 中去添加自动注册的功能，如自定义一样后处理器
     *  public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor{
     *      public Object postProcessBeforeInitialization(Object bean,String beanName){
     *          sout.printlen("================");
     *          return null;
     *      }
     *  }
     *  在配置文件中添加了配置
     *  <bean class="processors.MyInstantiationAwareBeanPostProcessor"></bean>
     *  那么使用 BeanFactory 方式进行 Spring 的 bean 的加载是不会有任何改变的，但是使用 ApplicationContext 方式获取 Bean 的时候会在获取
     *  每个 bean 的时打印出"==========" ,而这个特性就是在 registerBeanPostProcessors 方法中完成
     *  我们继续探索 registerBeanPostProcessors 的方法实现
     *
     *
     *
     */
    public static void invokeBeanFactoryPostProcessors(
            ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
        // Invoke BeanDefinitionRegistryPostProcessors first, if any.
        Set<String> processedBeans = new HashSet<String>();
		// 1.判断beanFactory是否为BeanDefinitionRegistry，beanFactory为DefaultListableBeanFactory,
		// 而DefaultListableBeanFactory实现了BeanDefinitionRegistry接口，因此这边为true ，对 BeanDefinitionRegistry 类型处理
        if (beanFactory instanceof BeanDefinitionRegistry) {
            LogUtils.info("invokeBeanFactoryPostProcessors beanFactory name :" + beanFactory.getClass().getName());

            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            // 记录通过硬编码的方式注册的 BeanFactoryPostProcessor 类型的处理器
            List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();
            // 记录通过硬编码的方式注册的 BeanDefinitionRegistryPostProcessor
            List<BeanDefinitionRegistryPostProcessor> registryPostProcessors = new LinkedList<BeanDefinitionRegistryPostProcessor>();
			// 遍历所有的beanFactoryPostProcessors, 将BeanDefinitionRegistryPostProcessor和普通BeanFactoryPostProcessor区分开
            for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				LogUtils.info("invokeBeanFactoryPostProcessors postProcessor name :" + postProcessor.getClass().getName());
                if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                    BeanDefinitionRegistryPostProcessor registryPostProcessor = (BeanDefinitionRegistryPostProcessor) postProcessor;
                    //调用它的后置方法 | 对于 beanDefinitionRegistryPostProcessor 类型，在 BeanFactoryPostProcessor 的基础上还有自己定义的方法需要先调用
                    registryPostProcessor.postProcessBeanDefinitionRegistry(registry);
                    //添加到我们用于保存的BeanDefinitionRegistryPostProcessor的集合中
                    registryPostProcessors.add(registryPostProcessor);
                } else {
                    //若没有实现BeanDefinitionRegistryPostProcessor接口，那么他就是BeanFactoryPostProcessor 把当前的后置处理器加入到regularPostProcessors中
                    // | 记录常规的 BeanFactoryPostProcessor
                    regularPostProcessors.add(postProcessor);
                }
            }

            // Do not initialize FactoryBeans here: We need to leave all regular beans
            // uninitialized to let the bean factory post-processors apply to them!
            // Separate between BeanDefinitionRegistryPostProcessors that implement
            // PriorityOrdered, Ordered, and the rest.
            //第一步:去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称 | 配置注册的后处理器
            String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			LogUtils.info("invokeBeanFactoryPostProcessors postProcessorNames  :" + Arrays.toString(postProcessorNames));
            // First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
            List<BeanDefinitionRegistryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanDefinitionRegistryPostProcessor>();
            for (String ppName : postProcessorNames) {
				LogUtils.info("invokeBeanFactoryPostProcessors ppName  :" + ppName);
                //判断是否实现了PriorityOrdered接口的
                if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                    //显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
                    priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    processedBeans.add(ppName);
                }
            }
            //对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
            sortPostProcessors(beanFactory, priorityOrderedPostProcessors);
            //把他加入到用于保存到registryProcessors中
            registryPostProcessors.addAll(priorityOrderedPostProcessors);
            /**
             * 在这里典型的BeanDefinitionRegistryPostProcessor就是ConfigurationClassPostProcessor
             * 用于进行bean定义的加载 比如我们的包扫描，@import等
             */
            invokeBeanDefinitionRegistryPostProcessors(priorityOrderedPostProcessors, registry);

            // Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
            postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			LogUtils.info("invokeBeanFactoryPostProcessors postProcessorNames  :" + Arrays.toString(postProcessorNames));
            List<BeanDefinitionRegistryPostProcessor> orderedPostProcessors = new ArrayList<BeanDefinitionRegistryPostProcessor>();
            for (String ppName : postProcessorNames) {
                //表示没有被处理过,且实现了Ordered接口的
                if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
                    //显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
                    orderedPostProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    //同时也加入到processedBeans集合中去
                    processedBeans.add(ppName);
                }
            }

            sortPostProcessors(beanFactory, orderedPostProcessors);

            registryPostProcessors.addAll(orderedPostProcessors);
            // 调用他的后置处理方法
            invokeBeanDefinitionRegistryPostProcessors(orderedPostProcessors, registry);

            // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
            // 调用没有实现任何优先级接口的BeanDefinitionRegistryPostProcessor
            // 定义一个重复处理的开关变量 默认值为true
            boolean reiterate = true;
            // 第一次就可以进来
            while (reiterate) {
                //进入循环马上把开关变量给改为false
                reiterate = false;
                //去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
                postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				LogUtils.info("invokeBeanFactoryPostProcessors postProcessorNames  :" + Arrays.toString(postProcessorNames));
                for (String ppName : postProcessorNames) {
                    //没有被处理过的
                    if (!processedBeans.contains(ppName)) {
                        //显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
                        BeanDefinitionRegistryPostProcessor pp = beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class);
                        registryPostProcessors.add(pp);
                        //同时也加入到processedBeans集合中去
                        processedBeans.add(ppName);
                        //同时也加入到processedBeans集合中去
                        pp.postProcessBeanDefinitionRegistry(registry);
                        //再次设置为true
                        reiterate = true;
                    }
                }
            }
            // Now, invoke the postProcessBeanFactory callback of all processors handled so far.
            // 调用实现了 BeanDefinitionRegistryPostProcessor 的接口 他也同时实现了BeanFactoryPostProcessor的方法
            // 激活 postProcessBeanFactory 方法，之前激活的是 postProcessBeanDefinitionRegistry
            // 硬编码设置 BeanDefinitionRegistryPostProcessor |  配置 BeanDefinitionRegistryPostProcessor
            invokeBeanFactoryPostProcessors(registryPostProcessors, beanFactory);
            //调用BeanFactoryPostProcessor | 常规 BeanFactoryPostProcessor
            invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
        } else {
            // Invoke factory processors registered with the context instance.
            //若当前的beanFactory没有实现了BeanDefinitionRegistry 直接调用beanFactoryPostProcessor接口的方法进行后置处理
            invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
        }

        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let the bean factory post-processors apply to them!
        //最后一步 获取容器中所有的 BeanFactoryPostProcessor | 常规的 BeanFactoryPostProcessor ，对于配置读取 BeanFactoryPostProcessor 的处理
        String[] postProcessorNames =
                beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);
		LogUtils.info("invokeBeanFactoryPostProcessors postProcessorNames  :" + Arrays.toString(postProcessorNames));
        // Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
        // Ordered, and the rest.
        //保存BeanFactoryPostProcessor类型实现了priorityOrdered
        List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
        //保存BeanFactoryPostProcessor类型实现了Ordered接口的
        List<String> orderedPostProcessorNames = new ArrayList<String>();
        //保存BeanFactoryPostProcessor没有实现任何优先级接口的
        List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
        // 对后处理进行分类
        for (String ppName : postProcessorNames) {
            LogUtils.info("invokeBeanFactoryPostProcessors ppName  :" + ppName);
            //processedBeans包含的话，表示在上面处理BeanDefinitionRegistryPostProcessor的时候处理过了 |  已经处理过了
            if (processedBeans.contains(ppName)) {
                // skip - already processed in first phase above
            //判断是否实现了PriorityOrdered
            } else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
            //判断是否实现了Ordered
            } else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
                orderedPostProcessorNames.add(ppName);
            //没有实现任何的优先级接口的
            } else {
                nonOrderedPostProcessorNames.add(ppName);
            }
        }

        // First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
        //先调用BeanFactoryPostProcessor实现了PriorityOrdered接口的 |  按照优先级进行排序
        sortPostProcessors(beanFactory, priorityOrderedPostProcessors);
        invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

        // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
        //再调用BeanFactoryPostProcessor实现了Ordered.
        List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
        for (String postProcessorName : orderedPostProcessorNames) {
            orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
        }
        // 按照order  进行排序
        sortPostProcessors(beanFactory, orderedPostProcessors);
        invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

        // Finally, invoke all other BeanFactoryPostProcessors.
        //调用没有实现任何方法接口的 | 无序，直接调用
        List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
        for (String postProcessorName : nonOrderedPostProcessorNames) {
            nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
        }
        invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
        // Clear cached merged bean definitions since the post-processors might have
        // modified the original metadata, e.g. replacing placeholders in values...
        beanFactory.clearMetadataCache();
    }

    public static void registerBeanPostProcessors(
            ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
        // 1.找出所有实现BeanPostProcessor接口的类
        String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
        LogUtils.info("registerBeanPostProcessors postProcessorNames : " + Arrays.toString(postProcessorNames));

        // Register BeanPostProcessorChecker that logs an info message when
        // a bean is created during BeanPostProcessor instantiation, i.e. when
        // a bean is not eligible for getting processed by all BeanPostProcessors.
        // BeanPostProcessor的目标计数 | BeanPostProcessorChecker 是一个普通的信息打印，可能会有些情况，
        // 当 Spring 的配置中的后处理器还没有被注册就已经开始了 bean 的初始化时
        //  便会打印出 beanPostProcessorChecker 中设定的信息

        int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
        LogUtils.info("registerBeanPostProcessors beanProcessorTargetCount : " +beanProcessorTargetCount);
        // 2.添加BeanPostProcessorChecker(主要用于记录信息)到beanFactory中
        beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

        // Separate between BeanPostProcessors that implement PriorityOrdered,
        // Ordered, and the rest.
        // 3.定义不同的变量用于区分: 实现PriorityOrdered接口的BeanPostProcessor、实现Ordered接口的BeanPostProcessor、普通BeanPostProcessor
        // 3.1 priorityOrderedPostProcessors: 用于存放实现PriorityOrdered接口的BeanPostProcessor  | 使用 PriorityOrdered   保证顺序
        List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
        // 3.2 internalPostProcessors: 用于存放Spring内部的BeanPostProcessor
        List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
        // 3.3 orderedPostProcessorNames: 用于存放实现Ordered接口的BeanPostProcessor的beanName |  使用 Ordered 保证顺序
        List<String> orderedPostProcessorNames = new ArrayList<String>();
        // 3.4 nonOrderedPostProcessorNames: 用于存放普通BeanPostProcessor的beanName |  无序 BeanPostProcessor
        List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
        // 4.遍历postProcessorNames, 将BeanPostProcessors按3.1 - 3.4定义的变量区分开
        for (String ppName : postProcessorNames) {
            LogUtils.info("registerBeanPostProcessors ppName : " + ppName);
            // 4.1 如果ppName对应的Bean实例实现了PriorityOrdered接口, 则拿到ppName对应的Bean实例并添加到priorityOrderedPostProcessors
            //  对于 BeanFactory 的注册，也不是直接注册就可以了，在 Spring 中支持对于 BeanPostProcessor 的排序，比如根据 PriorityOrderd
            // 进行排序，根据 Ordered 进行排序或者无序，而 Spring 在 BeanPostProcessor 的激活顺序的时候也会考虑对于顺序问题而先进行排序 的
            // 这里可能有个地方读者还是不是很理解，对于 internalPostProcessors 中存储的后处理器也就是 MergedBeanDefinitionPostProcessor 类型的
            // 处理器，在代码中似乎是被重复调用了。如：
            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
                priorityOrderedPostProcessors.add(pp);
                if (pp instanceof MergedBeanDefinitionPostProcessor) {
                    internalPostProcessors.add(pp);
                }
            } else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
                orderedPostProcessorNames.add(ppName);
            } else {
                nonOrderedPostProcessorNames.add(ppName);
            }
        }


        // First, register the BeanPostProcessors that implement PriorityOrdered.
        // 第一步，注册所有的实现PriorityOrdered的 BeanPostProcessor
        sortPostProcessors(beanFactory, priorityOrderedPostProcessors);
        registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

        // Next, register the BeanPostProcessors that implement Ordered.
        // 第二步 ，注册所有实现Ordered的BeanPostProcessor
        // 配合源码及注释，在 registerBeanPostProcessors 方法中所做的逻辑相信大家已经很清楚了，我们再做一些总结
        // 首先，我们会发现，对于 BeanPostProcessor 的处理与 BeanFactoryPostProcessor 的处理极为相似，但是似乎又有些不一样的地方
        //  经过反复的对比，我们发现，对于 BeanFactoryPostProcessor 的处理要区分两种情况，一种方式是通过硬编码的方式来处理，另一种
        // 是通过配置文件的方式来处理，那么为什么在 BeanPostProcessor 的处理只考虑了配置文件的而不考虑硬编码的方式呢？
        //  提取了这个问题，还是因为读者没有完全理解两者实现的功能，对于 BeanFactoryPostProcessor 的处理，不但要实现注册的功能，
        // 而且还要实现对后处理器的激活操作，所以需要载入配置的定义，并进行激活，而对于 BeanPostProcessor 并不需要马上调用，再说
        // 硬编码的方式实现功能只需要将配置文件的 BeanPostProcessor  提取出来并注册进入 beanFactory 就可以了
        List<BeanPostProcessor> orderedPostProcessors = new ArrayList<BeanPostProcessor>();
        for (String ppName : orderedPostProcessorNames) {
            LogUtils.info("registerBeanPostProcessors ppName : " + ppName);
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            orderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        sortPostProcessors(beanFactory, orderedPostProcessors);
        registerBeanPostProcessors(beanFactory, orderedPostProcessors);

        // Now, register all regular BeanPostProcessors.
        // 第三步，注册所有无序的 BeanPostProcessor
        List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
        for (String ppName : nonOrderedPostProcessorNames) {
            LogUtils.info("registerBeanPostProcessors ppName : " + ppName);
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            nonOrderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

        // Finally, re-register all internal BeanPostProcessors.
        // 第四步，注册所有 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessor ，并非重复注册
        sortPostProcessors(beanFactory, internalPostProcessors);
        registerBeanPostProcessors(beanFactory, internalPostProcessors);

        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
    }

    private static void sortPostProcessors(ConfigurableListableBeanFactory beanFactory, List<?> postProcessors) {
        Comparator<Object> comparatorToUse = null;
        if (beanFactory instanceof DefaultListableBeanFactory) {
            comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
        }
        if (comparatorToUse == null) {
            comparatorToUse = OrderComparator.INSTANCE;
        }
        Collections.sort(postProcessors, comparatorToUse);
    }

    /**
     * Invoke the given BeanDefinitionRegistryPostProcessor beans.
     */
    private static void invokeBeanDefinitionRegistryPostProcessors(
            Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

        for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessBeanDefinitionRegistry(registry);
        }
    }

    /**
     * Invoke the given BeanFactoryPostProcessor beans.
     */
    private static void invokeBeanFactoryPostProcessors(
            Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

        for (BeanFactoryPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessBeanFactory(beanFactory);
        }
    }

    /**
     * Register the given BeanPostProcessor beans.
     */
    private static void registerBeanPostProcessors(
            ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

        for (BeanPostProcessor postProcessor : postProcessors) {
            LogUtils.info("registerBeanPostProcessors postProcessor :" + postProcessor.getClass().getName());
            beanFactory.addBeanPostProcessor(postProcessor);
        }
    }


    /**
     * BeanPostProcessor that logs an info message when a bean is created during
     * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
     * getting processed by all BeanPostProcessors.
     */
    private static class BeanPostProcessorChecker implements BeanPostProcessor {

        private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

        private final ConfigurableListableBeanFactory beanFactory;

        private final int beanPostProcessorTargetCount;

        public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
            this.beanFactory = beanFactory;
            this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean != null && !(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
                    this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
                if (logger.isInfoEnabled()) {
                    logger.info("Bean '" + beanName + "' of type [" + bean.getClass() +
                            "] is not eligible for getting processed by all BeanPostProcessors " +
                            "(for example: not eligible for auto-proxying)");
                }
            }
            return bean;
        }

        private boolean isInfrastructureBean(String beanName) {
            if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
                BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
                return RootBeanDefinition.ROLE_INFRASTRUCTURE == bd.getRole();
            }
            return false;
        }
    }


    /**
     * BeanPostProcessor that detects beans which implement the ApplicationListener interface.
     * This catches beans that can't reliably be detected by getBeanNamesForType.
     */
    private static class ApplicationListenerDetector implements MergedBeanDefinitionPostProcessor, DestructionAwareBeanPostProcessor {

        private static final Log logger = LogFactory.getLog(ApplicationListenerDetector.class);

        private final AbstractApplicationContext applicationContext;

        private final Map<String, Boolean> singletonNames = new ConcurrentHashMap<String, Boolean>(64);

        public ApplicationListenerDetector(AbstractApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
            if (beanDefinition.isSingleton()) {
                this.singletonNames.put(beanName, Boolean.TRUE);
            }
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof ApplicationListener) {
                // potentially not detected as a listener by getBeanNamesForType retrieval
                Boolean flag = this.singletonNames.get(beanName);
                if (Boolean.TRUE.equals(flag)) {
                    // singleton bean (top-level or inner): register on the fly
                    this.applicationContext.addApplicationListener((ApplicationListener<?>) bean);
                } else if (flag == null) {
                    if (logger.isWarnEnabled() && !this.applicationContext.containsBean(beanName)) {
                        // inner bean with other scope - can't reliably process events
                        logger.warn("Inner bean '" + beanName + "' implements ApplicationListener interface " +
                                "but is not reachable for event multicasting by its containing ApplicationContext " +
                                "because it does not have singleton scope. Only top-level listener beans are allowed " +
                                "to be of non-singleton scope.");
                    }
                    this.singletonNames.put(beanName, Boolean.FALSE);
                }
            }
            return bean;
        }

        @Override
        public void postProcessBeforeDestruction(Object bean, String beanName) {
            if (bean instanceof ApplicationListener) {
                ApplicationEventMulticaster multicaster = this.applicationContext.getApplicationEventMulticaster();
                multicaster.removeApplicationListener((ApplicationListener<?>) bean);
                multicaster.removeApplicationListenerBean(beanName);
            }
        }
    }

}
