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

package org.springframework.jms.listener;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jms.Connection;
import javax.jms.JMSException;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.util.ClassUtils;

/**
 * Common base class for all containers which need to implement listening
 * based on a JMS Connection (either shared or freshly obtained for each attempt).
 * Inherits basic Connection and Session configuration handling from the
 * {@link org.springframework.jms.support.JmsAccessor} base class.
 *
 * <p>This class provides basic lifecycle management, in particular management
 * of a shared JMS Connection. Subclasses are supposed to plug into this
 * lifecycle, implementing the {@link #sharedConnectionEnabled()} as well
 * as the {@link #doInitialize()} and {@link #doShutdown()} template methods.
 *
 * <p>This base class does not assume any specific listener programming model
 * or listener invoker mechanism. It just provides the general runtime
 * lifecycle management needed for any kind of JMS-based listening mechanism
 * that operates on a JMS Connection/Session.
 *
 * <p>For a concrete listener programming model, check out the
 * {@link AbstractMessageListenerContainer} subclass. For a concrete listener
 * invoker mechanism, check out the {@link DefaultMessageListenerContainer} class.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #sharedConnectionEnabled()
 * @see #doInitialize()
 * @see #doShutdown()
 */
public abstract class AbstractJmsListeningContainer extends JmsDestinationAccessor
		implements BeanNameAware, DisposableBean, SmartLifecycle {

	private String clientId;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private String beanName;

	private Connection sharedConnection;

	private boolean sharedConnectionStarted = false;

	protected final Object sharedConnectionMonitor = new Object();

	private boolean active = false;

	private boolean running = false;

	private final List<Object> pausedTasks = new LinkedList<Object>();

	protected final Object lifecycleMonitor = new Object();


	/**
	 * Specify the JMS client ID for a shared Connection created and used
	 * by this container.
	 * <p>Note that client IDs need to be unique among all active Connections
	 * of the underlying JMS provider. Furthermore, a client ID can only be
	 * assigned if the original ConnectionFactory hasn't already assigned one.
	 * @see javax.jms.Connection#setClientID
	 * @see #setConnectionFactory
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Return the JMS client ID for the shared Connection created and used
	 * by this container, if any.
	 */
	public String getClientId() {
		return this.clientId;
	}

	/**
	 * Set whether to automatically start the container after initialization.
	 * <p>Default is "true"; set this to "false" to allow for manual startup
	 * through the {@link #start()} method.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Specify the phase in which this container should be started and
	 * stopped. The startup order proceeds from lowest to highest, and
	 * the shutdown order is the reverse of that. By default this value
	 * is Integer.MAX_VALUE meaning that this container starts as late
	 * as possible and stops as soon as possible.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Return the phase in which this container will be started and stopped.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Return the bean name that this listener container has been assigned
	 * in its containing bean factory, if any.
	 */
	protected final String getBeanName() {
		return this.beanName;
	}


	/**
	 * Delegates to {@link #validateConfiguration()} and {@link #initialize()}.
	 * 13.3.2 监听器容器
	 * 消息监听器容器是一个用于查看JMS目标等待消息到达特殊的bean ，一旦消息到达它就可以获取消息，并通过调用onMessage()方法将消息传递给
	 * 一个MessageListener实现，Spring中消息监听器容器的类型如下：
	 * SimpleMessageListenerContainer： 最简单的消息监听器容器，只能处理固定数量的JMS会话，且不支持事务
	 * DefaultMessageListenerContainer:这个消息监听器容器建立在SimpleMessageListenerContainer容器之上，添加了对事务的支持
	 * serversession.ServerSessionMessage.ListenerContainer：这是功能最强大的消息监听器，与DefaultMessageListenerContainer 相同
	 * 它支持事务，但是它还允许动态地管理JMS会话。
	 * 下面以DefaultMessageListenerContainer为例进行分析，看看消息监听器容器的实现，在之前的消息监听器的使用示例中，我们了解到了在使用
	 * 消息监听器容器时，一定要将自定义消息监听器置入到容器中，这样才可以在收到信息时，容器把消息转向监听器处理，查看DefaultMessageListenerContainer
	 * 层次结构图，如图 13-2 所示
	 * 同样，我们看此类实现InitializingBean接口，按照以往的风格，我们还是首先查看接口方法afterPropertiesSet()中的逻辑，其方法的实现在其
	 * 父类AbstractJmsListeningContainer中
	 *
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		// 验证配置文件
		validateConfiguration();
		// 初始化
		initialize();
	}

	/**
	 * Validate the configuration of this container.
	 * <p>The default implementation is empty. To be overridden in subclasses.
	 */
	protected void validateConfiguration() {
	}

	/**
	 * Calls {@link #shutdown()} when the BeanFactory destroys the container instance.
	 * @see #shutdown()
	 */
	@Override
	public void destroy() {
		shutdown();
	}


	//-------------------------------------------------------------------------
	// Lifecycle methods for starting and stopping the container
	//-------------------------------------------------------------------------

	/**
	 * Initialize this container.
	 * <p>Creates a JMS Connection, starts the {@link javax.jms.Connection}
	 * (if {@link #setAutoStartup(boolean) "autoStartup"} hasn't been turned off),
	 * and calls {@link #doInitialize()}.
	 * @throws org.springframework.jms.JmsException if startup failed
	 * 监听器容器的初始化只包含了三句代码，其中前两句只用于属性的验证，比如connectionFactory或者destination等属性是否为空等
	 * 而真正的用于初始化的操作委托在initialize()中执行
	 * 这里用到了concurrentConsumers属性，网络中对此属性用法说明如下：
	 * 消息监听器允许创建多个Session和MessageConsumer来接收消息，具体的个数由concurrentConsumers属性指定，需要注意的是，应该只是
	 * 在Destination为Queue的时候才使用多个MessageConsumer（Queue中的一个消息只能被一个Consumer接收），虽然使用了多个MessageConsumer
	 * 会提高消息处理的性能，但是消息处理的顺序却得不到保证，消息被接收的顺序仍然是消息发送时的有顺序，但是由于消息可能会被并发处理，因此，
	 * 消息处理的顺序可能和消息发送的顺序不同，此外，不应该在Destination为Topic的时候使用多个MessageConsumer因为多个MessageConsumer
	 * 会接收到同样的消息
	 *
	 */
	public void initialize() throws JmsException {
		try {
			// lifecycleMonitor用于控制生命周期的同步处理
			synchronized (this.lifecycleMonitor) {
				this.active = true;
				this.lifecycleMonitor.notifyAll();
			}
			doInitialize();
		}
		catch (JMSException ex) {
			synchronized (this.sharedConnectionMonitor) {
				ConnectionFactoryUtils.releaseConnection(this.sharedConnection, getConnectionFactory(), this.autoStartup);
				this.sharedConnection = null;
			}
			throw convertJmsAccessException(ex);
		}
	}

	/**
	 * Stop the shared Connection, call {@link #doShutdown()},
	 * and close this container.
	 * @throws JmsException if shutdown failed
	 */
	public void shutdown() throws JmsException {
		logger.debug("Shutting down JMS listener container");
		boolean wasRunning;
		synchronized (this.lifecycleMonitor) {
			wasRunning = this.running;
			this.running = false;
			this.active = false;
			this.pausedTasks.clear();
			this.lifecycleMonitor.notifyAll();
		}

		// Stop shared Connection early, if necessary.
		if (wasRunning && sharedConnectionEnabled()) {
			try {
				stopSharedConnection();
			}
			catch (Throwable ex) {
				logger.debug("Could not stop JMS Connection on shutdown", ex);
			}
		}

		// Shut down the invokers.
		try {
			doShutdown();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			if (sharedConnectionEnabled()) {
				synchronized (this.sharedConnectionMonitor) {
					ConnectionFactoryUtils.releaseConnection(this.sharedConnection, getConnectionFactory(), false);
					this.sharedConnection = null;
				}
			}
		}
	}

	/**
	 * Return whether this container is currently active,
	 * that is, whether it has been set up but not shut down yet.
	 */
	public final boolean isActive() {
		synchronized (this.lifecycleMonitor) {
			return this.active;
		}
	}

	/**
	 * Start this container.
	 * @throws JmsException if starting failed
	 * @see #doStart
	 */
	@Override
	public void start() throws JmsException {
		try {
			doStart();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
	}

	/**
	 * Start the shared Connection, if any, and notify all invoker tasks.
	 * @throws JMSException if thrown by JMS API methods
	 * @see #startSharedConnection
	 */
	protected void doStart() throws JMSException {
		// Lazily establish a shared Connection, if necessary.
		if (sharedConnectionEnabled()) {
			establishSharedConnection();
		}

		// Reschedule paused tasks, if any.
		synchronized (this.lifecycleMonitor) {
			this.running = true;
			this.lifecycleMonitor.notifyAll();
			resumePausedTasks();
		}

		// Start the shared Connection, if any.
		if (sharedConnectionEnabled()) {
			startSharedConnection();
		}
	}

	/**
	 * Stop this container.
	 * @throws JmsException if stopping failed
	 * @see #doStop
	 */
	@Override
	public void stop() throws JmsException {
		try {
			doStop();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	/**
	 * Notify all invoker tasks and stop the shared Connection, if any.
	 * @throws JMSException if thrown by JMS API methods
	 * @see #stopSharedConnection
	 */
	protected void doStop() throws JMSException {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.lifecycleMonitor.notifyAll();
		}

		if (sharedConnectionEnabled()) {
			stopSharedConnection();
		}
	}

	/**
	 * Determine whether this container is currently running,
	 * that is, whether it has been started and not stopped yet.
	 * @see #start()
	 * @see #stop()
	 * @see #runningAllowed()
	 */
	@Override
	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return (this.running && runningAllowed());
		}
	}

	/**
	 * Check whether this container's listeners are generally allowed to run.
	 * <p>This implementation always returns {@code true}; the default 'running'
	 * state is purely determined by {@link #start()} / {@link #stop()}.
	 * <p>Subclasses may override this method to check against temporary
	 * conditions that prevent listeners from actually running. In other words,
	 * they may apply further restrictions to the 'running' state, returning
	 * {@code false} if such a restriction prevents listeners from running.
	 */
	protected boolean runningAllowed() {
		return true;
	}


	//-------------------------------------------------------------------------
	// Management of a shared JMS Connection
	//-------------------------------------------------------------------------

	/**
	 * Establish a shared Connection for this container.
	 * <p>The default implementation delegates to {@link #createSharedConnection()},
	 * which does one immediate attempt and throws an exception if it fails.
	 * Can be overridden to have a recovery process in place, retrying
	 * until a Connection can be successfully established.
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected void establishSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			if (this.sharedConnection == null) {
				this.sharedConnection = createSharedConnection();
				logger.debug("Established shared JMS Connection");
			}
		}
	}

	/**
	 * Refresh the shared Connection that this container holds.
	 * <p>Called on startup and also after an infrastructure exception
	 * that occurred during invoker setup and/or execution.
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected final void refreshSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			ConnectionFactoryUtils.releaseConnection(
					this.sharedConnection, getConnectionFactory(), this.sharedConnectionStarted);
			this.sharedConnection = null;
			this.sharedConnection = createSharedConnection();
			if (this.sharedConnectionStarted) {
				this.sharedConnection.start();
			}
		}
	}

	/**
	 * Create a shared Connection for this container.
	 * <p>The default implementation creates a standard Connection
	 * and prepares it through {@link #prepareSharedConnection}.
	 * @return the prepared Connection
	 * @throws JMSException if the creation failed
	 */
	protected Connection createSharedConnection() throws JMSException {
		Connection con = createConnection();
		try {
			prepareSharedConnection(con);
			return con;
		}
		catch (JMSException ex) {
			JmsUtils.closeConnection(con);
			throw ex;
		}
	}

	/**
	 * Prepare the given Connection, which is about to be registered
	 * as shared Connection for this container.
	 * <p>The default implementation sets the specified client id, if any.
	 * Subclasses can override this to apply further settings.
	 * @param connection the Connection to prepare
	 * @throws JMSException if the preparation efforts failed
	 * @see #getClientId()
	 */
	protected void prepareSharedConnection(Connection connection) throws JMSException {
		String clientId = getClientId();
		if (clientId != null) {
			connection.setClientID(clientId);
		}
	}

	/**
	 * Start the shared Connection.
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.Connection#start()
	 */
	protected void startSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			this.sharedConnectionStarted = true;
			if (this.sharedConnection != null) {
				try {
					this.sharedConnection.start();
				}
				catch (javax.jms.IllegalStateException ex) {
					logger.debug("Ignoring Connection start exception - assuming already started: " + ex);
				}
			}
		}
	}

	/**
	 * Stop the shared Connection.
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.Connection#start()
	 */
	protected void stopSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			this.sharedConnectionStarted = false;
			if (this.sharedConnection != null) {
				try {
					this.sharedConnection.stop();
				}
				catch (javax.jms.IllegalStateException ex) {
					logger.debug("Ignoring Connection stop exception - assuming already stopped: " + ex);
				}
			}
		}
	}

	/**
	 * Return the shared JMS Connection maintained by this container.
	 * Available after initialization.
	 * @return the shared Connection (never {@code null})
	 * @throws IllegalStateException if this container does not maintain a
	 * shared Connection, or if the Connection hasn't been initialized yet
	 * @see #sharedConnectionEnabled()
	 */
	protected final Connection getSharedConnection() {
		if (!sharedConnectionEnabled()) {
			throw new IllegalStateException(
					"This listener container does not maintain a shared Connection");
		}
		synchronized (this.sharedConnectionMonitor) {
			if (this.sharedConnection == null) {
				throw new SharedConnectionNotInitializedException(
						"This listener container's shared Connection has not been initialized yet");
			}
			return this.sharedConnection;
		}
	}


	//-------------------------------------------------------------------------
	// Management of paused tasks
	//-------------------------------------------------------------------------

	/**
	 * Take the given task object and reschedule it, either immediately if
	 * this container is currently running, or later once this container
	 * has been restarted.
	 * <p>If this container has already been shut down, the task will not
	 * get rescheduled at all.
	 * @param task the task object to reschedule
	 * @return whether the task has been rescheduled
	 * (either immediately or for a restart of this container)
	 * @see #doRescheduleTask
	 */
	protected final boolean rescheduleTaskIfNecessary(Object task) {
		if (this.running) {
			try {
				// 分析源码得知，根据concurrentConsumers数据建立了对应的数量的线程，即使读者不了解线程池的使用，至少根据以上的代码
				// 可以推断出doRescheduleTask函数其实是在开启一个线程执行Runnable，我们反追踪这个传入的参数，可以看到这个参数其实是
				// AsyncMessageListenerInvoker 类型的实例，因此我们可以推断，Spring根据concurrentConsumers数量建立了对应的数量的线程，
				// 而每个线程都作为一个独立的接收者在循环接收消息
				// 于是我们把所有的焦点转向AsyncMessageListenerInvoker这个类的实现，由于它是作为一个Runnable角色去执行的。所以
				// 对以这个类的分析从run方法开始
				doRescheduleTask(task);
			}
			catch (RuntimeException ex) {
				logRejectedTask(task, ex);
				this.pausedTasks.add(task);
			}
			return true;
		}
		else if (this.active) {
			this.pausedTasks.add(task);
			return true;

		}
		else {
			return false;
		}
	}

	/**
	 * Try to resume all paused tasks.
	 * Tasks for which rescheduling failed simply remain in paused mode.
	 */
	protected void resumePausedTasks() {
		synchronized (this.lifecycleMonitor) {
			if (!this.pausedTasks.isEmpty()) {
				for (Iterator<?> it = this.pausedTasks.iterator(); it.hasNext();) {
					Object task = it.next();
					try {
						doRescheduleTask(task);
						it.remove();
						if (logger.isDebugEnabled()) {
							logger.debug("Resumed paused task: " + task);
						}
					}
					catch (RuntimeException ex) {
						logRejectedTask(task, ex);
						// Keep the task in paused mode...
					}
				}
			}
		}
	}

	/**
	 * Determine the number of currently paused tasks, if any.
	 */
	public int getPausedTaskCount() {
		synchronized (this.lifecycleMonitor) {
			return this.pausedTasks.size();
		}
	}

	/**
	 * Reschedule the given task object immediately.
	 * <p>To be implemented by subclasses if they ever call
	 * {@code rescheduleTaskIfNecessary}.
	 * This implementation throws an UnsupportedOperationException.
	 * @param task the task object to reschedule
	 * @see #rescheduleTaskIfNecessary
	 */
	protected void doRescheduleTask(Object task) {
		throw new UnsupportedOperationException(
				ClassUtils.getShortName(getClass()) + " does not support rescheduling of tasks");
	}

	/**
	 * Log a task that has been rejected by {@link #doRescheduleTask}.
	 * <p>The default implementation simply logs a corresponding message
	 * at debug level.
	 * @param task the rejected task object
	 * @param ex the exception thrown from {@link #doRescheduleTask}
	 */
	protected void logRejectedTask(Object task, RuntimeException ex) {
		if (logger.isDebugEnabled()) {
			logger.debug("Listener container task [" + task + "] has been rejected and paused: " + ex);
		}
	}


	//-------------------------------------------------------------------------
	// Template methods to be implemented by subclasses
	//-------------------------------------------------------------------------

	/**
	 * Return whether a shared JMS Connection should be maintained
	 * by this container base class.
	 * @see #getSharedConnection()
	 */
	protected abstract boolean sharedConnectionEnabled();

	/**
	 * Register any invokers within this container.
	 * <p>Subclasses need to implement this method for their specific
	 * invoker management process.
	 * <p>A shared JMS Connection, if any, will already have been
	 * started at this point.
	 * @throws JMSException if registration failed
	 * @see #getSharedConnection()
	 */
	protected abstract void doInitialize() throws JMSException;

	/**
	 * Close the registered invokers.
	 * <p>Subclasses need to implement this method for their specific
	 * invoker management process.
	 * <p>A shared JMS Connection, if any, will automatically be closed
	 * <i>afterwards</i>.
	 * @throws JMSException if shutdown failed
	 * @see #shutdown()
	 */
	protected abstract void doShutdown() throws JMSException;


	/**
	 * Exception that indicates that the initial setup of this container's
	 * shared JMS Connection failed. This is indicating to invokers that they need
	 * to establish the shared Connection themselves on first access.
	 */
	@SuppressWarnings("serial")
	public static class SharedConnectionNotInitializedException extends RuntimeException {

		/**
		 * Create a new SharedConnectionNotInitializedException.
		 * @param msg the detail message
		 */
		protected SharedConnectionNotInitializedException(String msg) {
			super(msg);
		}
	}

}
