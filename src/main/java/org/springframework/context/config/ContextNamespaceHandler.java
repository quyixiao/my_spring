/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.context.annotation.AnnotationConfigBeanDefinitionParser;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;

/**
 * {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * for the '{@code context}' namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 */
public class ContextNamespaceHandler extends NamespaceHandlerSupport {

	/***
	 * 7.5  创建 AOP 静态代理
	 * AOP 是静态代理主要的在虚拟机启动的时候改变目标对象字节码的方式来完成对象目标对象的增强，它与动态代理相比具有更高的效率，因为在
	 * 动态代理调用的过程中，还需要一个动态创建代理类并代理目标对象的步骤，而静态代理则是在启动时便完成了字节码的增强，当系统再次调用的
	 * 目标类时与调用正常的类并无差别，所以在效率上会相对高一些
	 * 7.5.1 Instrumentation 的使用
	 * Java 在1.5 引入 了 java.lang.instrument，你可以由此实现一个 java agent , 通过此 agent 来修改类的字节码即改变一个类，本节
	 * 会通过 java Instrument 实现一个简单的 profiler，当然 instrument 并不限于 profiler，instrument 它可以做很多的事情
	 * 它类似于一种更加低级，更松耦合的 AOP ，可以从底层来改变一个类的行为，你可以由此产生无限的遐想，接下来要做的事情，就是计算一个
	 * 办法所花的时间，通常我们会在代码中按以下的方式编写
	 * 在方法的开头加入 long stime = System.nanoTime() ，在方法的结尾通过 System.nanoTime() - startTime  得出方法所花的时间
	 * 你不得不想监控的每个方法中写入了重复的代码，好一点的情况，你可以用 AOP 来干这种事情，但是部感觉有点别扭，这种 profiler 的代码还是要
	 * 打包在你的项目中，java Instrument 使得这一切都更加的干净
	 * public class PerfMonxformer implements ClassFileTransformer {
	 *
	 *     public byte [] transform(ClassLoader loader ,String className,Class<?> classBeingRedefined ,ProtectionDomain protectionDomain ,
	 *     byte [] classFileBuffer ) throws IllegalClassFormatException {
	 *         byte [] transformed = null;
	 *         System.out.println("Transforming " + className );
	 *         ClassPool pool = ClassPool.getDefault();
	 *         CtClass cl = null;
	 *         try{
	 *            cl = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
	 *            if(cl.isInterface() == false){
	 *                CtBehavior[] methods = cl.getDeclareBehaviors();
	 *                for(int i = 0 ;i < method.length;i ++){
	 *                    if(methods[i].isEmpty() == false){
	 *                        // 修改 method 的字节码
	 *                        doMethod(method[i]);
	 *                    }
	 *                }
	 *            }
	 *         }catch(Exception e ){
	 *             System.err.pringln("Could not instrument " + className + ", exception :" + e.getMessage());
	 *         }finally{
	 *             if(cl != null){
	 *                 cl.detach();
	 *             }
	 *         }
	 *     }
	 *
	 *
	 *     private void doMethod(CtBehavior method ) throws Excption{
	 *         method.insertBefore("long startTime = System.nanoTimes();");
	 *         method.insertAfter("System.out.println(/"leave " +  method.getName() + " and time :/+ (System.nanoTime() - startTime)" )");
	 *     }
	 * }
	 *
	 *
	 * (2)编写 agent 类
	 * public class PerfMonAgent{
	 *     static private Instrumentation inst = null;
	 *     // this method is called before the application main method is called
	 *     // when this agent is specified to java VM
	 *    	public static void premain(String agentArgs,Instrumentation _inst ){
	 *    	   System.out.println("PerfMonAgent.premain() was called ");
	 *    	   // Initialize the static variables we use to track information
	 *    	   inst = _inst ;
	 *    	   // Set up the class-file transformer
	 *    	   ClassFileTransformer trans = new PerfMonXformer ();
	 *    	   System.out.println("Adding a PerfMonXformer instance to th JVM .");
	 *    	   inst.addTransformer(trans);
	 *    	}
	 * }
	 * 上面的两个类是 agent 的核心了，JVM 启动时在应用加载前会调用 PerfMonAgent.premain， 然后 PerfMonAgentpremain 中实例化一个定制的
	 * ClassFileTransforme，即 PerfMonXformer 并通过 inst.addTransformer(trans) 把 PerfMonXFormer.transForm 都会被调用，你在此
	 * 方法中可以改变加载的类，真的是很神奇，为了改变类的字节码，我们使用了 JBosss 的 javassist ,虽然你不一定用，但JBoss 的 javassist
	 *  真的很强大，能让你很容易的改变类的字节码，在上面的方法中，我们通过改变类的字节码，在每个类的方法中加入了 long startTime = System.nanoTime();
	 *  在方法的出口加入了：
	 *  System.out.println("methodClassName.methodName:" + (System.nanoTime() - startTime()));
	 *  (3).打包 agent.
	 *  对于 agent 的打包，有点讲究
	 *  	 JAR 的 META-INF/MANIFEST.MF 加入 Premain-Class:xxx :xxx 在此语境中就是我们的 agent 类，即 org.toy.PerfMonAgent
	 *  	  如果你的 agent 类引入别的包，需要使用 Boot-Class-Path:xx ，xx 在此语境中就是上面的提到的 JBoss javassit ,
	 *  	 即/home/pwlazy/.m2/repository/javassist/3.8.0.GA/javassist-3.8.0.GA.jar
	 * 下面附上 Maven 的 POM
	 *  <?xml version="1.0" encoding="UTF-8"?>
	 * <project xmlns="http://maven.apache.org/POM/4.0.0"
	 *          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 *          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	 *     <modelVersion>4.0.0</modelVersion>
	 *     <groupId>org.toy</groupId>
	 *     <artifactId>jar</artifactId>
	 *     <version>1.0-SNAPSHOT</version>
	 *     <name>toy-inst</name>
	 *     <url>http://maven.apache.org</url>
	 *     <dependencies>
	 *         <dependency>
	 *             <groupId>javassist</groupId>
	 *             <artifactId>javassist</artifactId>
	 *             <version>3.8.0.GA</version>
	 *         </dependency>
	 *         <dependency>
	 *             <groupId>junit</groupId>
	 *             <artifactId>junit</artifactId>
	 *             <version>3.8.1</version>
	 *             <scop>test</scop>
	 *         </dependency>
	 *         <build>
	 *             <plugins>
	 *                 <plugin>
	 *                     <groupId>org.apache.maven.plugins</groupId>
	 *                     <artifactId>maven-jar-plugin</artifactId>
	 *                     <version>2.2</version>
	 *                     <configuration>
	 *                         <archive>
	 *                             <manifestEntries>
	 *                                 <Premain-Class>org.toy.PerfMonAgent</Premain-Class>
	 *                                 <Boot-Class-Path>/home/quyixiao/.m2/repository/javassist/javassist/3.8.0.GA/javassist-3.8.0.GA.jar</Boot-Class-Path>
	 *                             </manifestEntries>
	 *                         </archive>
	 *                     </configuration>
	 *                 </plugin>
	 *                 <plugin>
	 *                     <artifactId>maven-compiler-plugin</artifactId>
	 *                     <configuration>
	 *                         <source>1.6</source>
	 *                         <target>1.6</target>
	 *                     </configuration>
	 *                 </plugin>
	 *             </plugins>
	 *         </build>
	 *     </dependencies>
	 *
	 * (4) 打包应用
	 * public class App{
	 *     public static void main(String [] args){
	 *         new App().test();
	 *     }
	 *     public void test(){
	 *         System.out.println("Hello World!");
	 *     }
	 * }
	 * JAVA选项中有-javaagent:xx 其中 xx就是你的 agentJAR ，Java 通过此选项加载 agent ，由 agent 来监控 classpath 下的应用
	 *  最后的结果
	 *  PerfMonAgent.premain() was called
	 *  Adding a PerfMonXformer instance to the JVM .
	 *  Transforming org/toy/App
	 *  Hello World !
	 *  java.io.PringStream.pring:314216
	 *  org.toy.App.test:540082
	 *  Transforming java/lang/Shutdown
	 *  Transforming java/lang/Shutdwon$Lock
	 *  java.lang.Shutdown.runHooks:29124
	 *  java.lang.Shutdown.sequence:132768
	 *  由执行的结果可以看出，执行的顺序以及通过改变 org.toy.App 的字节码加入监控代码确实生效了，你也可以发现，通过 Instrument 实现
	 *  agent 使得监控代码和应用代码完全隔离了
	 *  通过之前的两个示例，我们似乎已经有了体会，在 Spring中静态 AOP 直接使用了 AspectJ 提供的方法，而 AspectJ 又在 Instrument
	 *  基础上进行了封装，就以上的示例来看，至少在 AspectJ 中会有如下的功能
	 *  （1） 读取 META-INF/aop.xml
	 *   (2) 当 aop.xml 中定义了增强器通过自定义的 ClassFileTransformer 织入对应的类中
	 *   当然这都是 AspectJ 所做的事情，并不是我们讨论的范畴，Spring直接使用 AspectJ ，也就是将动态代理的任务直接委托给了 AspectJ
	 *   ，那么 Spring 怎样嵌入 AspectJ 的呢？  同样我们还是从配置文件入手
	 *
	 *   7.5.2  自定义标签
	 *   在 Spring 中如果要使用 AspectJ 的功能，首先要做的第一步就是在配置文件中加入配置，<context:load-time-weaver/>
	 *   我们根据之前介绍的自定义命名空间的知识便可以推断，引用 AspectJ 的入口便是在这里，可以通过查找 load-time-weaver 来找对应的自定义的
	 *   处理类
	 *   通过 Eclipse 提供的字符串搜索功能，我们找到了 ContextNameHandler 在其中有这样的一段函数
	 *
	 *
	 */
	@Override
	public void init() {
		registerBeanDefinitionParser("property-placeholder", new PropertyPlaceholderBeanDefinitionParser());
		registerBeanDefinitionParser("property-override", new PropertyOverrideBeanDefinitionParser());
		registerBeanDefinitionParser("annotation-config", new AnnotationConfigBeanDefinitionParser());
		registerBeanDefinitionParser("component-scan", new ComponentScanBeanDefinitionParser());
		registerBeanDefinitionParser("load-time-weaver", new LoadTimeWeaverBeanDefinitionParser());
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
		registerBeanDefinitionParser("mbean-export", new MBeanExportBeanDefinitionParser());
		registerBeanDefinitionParser("mbean-server", new MBeanServerBeanDefinitionParser());
	}

}
