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

    public static void invokeBeanFactoryPostProcessors(
            ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

        // Invoke BeanDefinitionRegistryPostProcessors first, if any.
        Set<String> processedBeans = new HashSet<String>();
		// 1.判断beanFactory是否为BeanDefinitionRegistry，beanFactory为DefaultListableBeanFactory,
		// 而DefaultListableBeanFactory实现了BeanDefinitionRegistry接口，因此这边为true
        if (beanFactory instanceof BeanDefinitionRegistry) {
            LogUtils.info("invokeBeanFactoryPostProcessors beanFactory name :" + beanFactory.getClass().getName());

            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// 用于存放普通的BeanFactoryPostProcessor
            List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();
			// 用于存放BeanDefinitionRegistryPostProcessorinvokeBeanFactoryPostProcessors
            List<BeanDefinitionRegistryPostProcessor> registryPostProcessors = new LinkedList<BeanDefinitionRegistryPostProcessor>();
			// 遍历所有的beanFactoryPostProcessors, 将BeanDefinitionRegistryPostProcessor和普通BeanFactoryPostProcessor区分开
            for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				LogUtils.info("invokeBeanFactoryPostProcessors postProcessor name :" + postProcessor.getClass().getName());
                if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                    BeanDefinitionRegistryPostProcessor registryPostProcessor = (BeanDefinitionRegistryPostProcessor) postProcessor;
                    //调用它的后置方法
                    registryPostProcessor.postProcessBeanDefinitionRegistry(registry);
                    //添加到我们用于保存的BeanDefinitionRegistryPostProcessor的集合中
                    registryPostProcessors.add(registryPostProcessor);
                } else {
                    //若没有实现BeanDefinitionRegistryPostProcessor接口，那么他就是BeanFactoryPostProcessor 把当前的后置处理器加入到regularPostProcessors中
                    regularPostProcessors.add(postProcessor);
                }
            }

            // Do not initialize FactoryBeans here: We need to leave all regular beans
            // uninitialized to let the bean factory post-processors apply to them!
            // Separate between BeanDefinitionRegistryPostProcessors that implement
            // PriorityOrdered, Ordered, and the rest.
            //第一步:去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
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
            //调用他的后置处理方法
            invokeBeanDefinitionRegistryPostProcessors(orderedPostProcessors, registry);

            // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
            //调用没有实现任何优先级接口的BeanDefinitionRegistryPostProcessor
            //定义一个重复处理的开关变量 默认值为true
            boolean reiterate = true;
            //第一次就可以进来
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
            //调用实现了BeanDefinitionRegistryPostProcessor的接口 他是他也同时实现了BeanFactoryPostProcessor的方法
            invokeBeanFactoryPostProcessors(registryPostProcessors, beanFactory);
            //调用BeanFactoryPostProcessor
            invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
        } else {
            // Invoke factory processors registered with the context instance.
            //若当前的beanFactory没有实现了BeanDefinitionRegistry 直接调用beanFactoryPostProcessor接口的方法进行后置处理
            invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
        }

        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let the bean factory post-processors apply to them!
        //最后一步 获取容器中所有的 BeanFactoryPostProcessor
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
        for (String ppName : postProcessorNames) {
            LogUtils.info("invokeBeanFactoryPostProcessors ppName  :" + ppName);
            //processedBeans包含的话，表示在上面处理BeanDefinitionRegistryPostProcessor的时候处理过了
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
        //先调用BeanFactoryPostProcessor实现了PriorityOrdered接口的
        sortPostProcessors(beanFactory, priorityOrderedPostProcessors);
        invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

        // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
        //再调用BeanFactoryPostProcessor实现了Ordered.
        List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
        for (String postProcessorName : orderedPostProcessorNames) {
            orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
        }
        sortPostProcessors(beanFactory, orderedPostProcessors);
        invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

        // Finally, invoke all other BeanFactoryPostProcessors.
        //调用没有实现任何方法接口的
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
        // BeanPostProcessor的目标计数
        int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
        LogUtils.info("registerBeanPostProcessors beanProcessorTargetCount : " +beanProcessorTargetCount);
        // 2.添加BeanPostProcessorChecker(主要用于记录信息)到beanFactory中
        beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

        // Separate between BeanPostProcessors that implement PriorityOrdered,
        // Ordered, and the rest.
        // 3.定义不同的变量用于区分: 实现PriorityOrdered接口的BeanPostProcessor、实现Ordered接口的BeanPostProcessor、普通BeanPostProcessor
        // 3.1 priorityOrderedPostProcessors: 用于存放实现PriorityOrdered接口的BeanPostProcessor
        List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
        // 3.2 internalPostProcessors: 用于存放Spring内部的BeanPostProcessor
        List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
        // 3.3 orderedPostProcessorNames: 用于存放实现Ordered接口的BeanPostProcessor的beanName
        List<String> orderedPostProcessorNames = new ArrayList<String>();
        // 3.4 nonOrderedPostProcessorNames: 用于存放普通BeanPostProcessor的beanName
        List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
        // 4.遍历postProcessorNames, 将BeanPostProcessors按3.1 - 3.4定义的变量区分开
        for (String ppName : postProcessorNames) {
            LogUtils.info("registerBeanPostProcessors ppName : " + ppName);
            // 4.1 如果ppName对应的Bean实例实现了PriorityOrdered接口, 则拿到ppName对应的Bean实例并添加到priorityOrderedPostProcessors
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
        sortPostProcessors(beanFactory, priorityOrderedPostProcessors);
        registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

        // Next, register the BeanPostProcessors that implement Ordered.
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
