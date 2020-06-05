/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	/**
	 * 创建AOP代理对象
	 * @param config the AOP configuration in the form of an
	 * AdvisedSupport object
	 * @return
	 * @throws AopConfigException
	 */
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		//todo 在这里判断代理的设置属性，
		// optimize：代理是否应该执行积极的优化，默认为false
		// proxyTargetClass：是否直接代理目标类以及任何接口
		// hasNoUserSuppliedProxyInterfaces: 判断是否又用户提供的代理接口
		// (1)optimize: 用来控制通过CGLIB创建的代理是否使用激进的优化策略，除非完全了解AOP如何进行优化的，则不应该设置这个值（这个值只对CGLIB代理方法有用）</br>
		// (2)proxyTargetClass: 这个属性为true时，目标类本身被代理而不是目标类的接口，如果这个属性值被设置为true，则使用CGLIB</br>
		// (3)hasNoUserSuppliedProxyInterfaces：是否存在用户自定义的代理的接口</br>
		// 如果目标实现了接口，可以使用JDK代理（默认），也可以强制使用CGLIB方法进行代理的创建，如果目标对象没有实现接口，则必须使用CGLIB库。</br>
		// JDK动态代理只能对实现了接口的类生成代理，而不能针对类。</br>
		// CGLIB是针对类实现代理，主要是对指定的类生成一个子类，覆盖其中的方法，因为是继承，所以这个类或方法最好不要声明为final类型的。</br>
		// 对于JDK的动态代理使用，我们需要自定义一个类实现InvocationHandler，并实现其中需要重写的3个函数：1.构造函数，将代理的对象传入；2.invoke方法，此方法中实现了AOP增强的所有逻辑；3.getProxy方法。同理spring使用JDK的动态代理同样需要用这种方式，因此JdkDynamicAopProxy类实现了InvocationHandler类并且会在invoke方法中把AOP的核心逻辑写在其中
		//
		/**
		 * 如果AOP使用显式优化，或者配置了目标类，或者只使用Spring支持的代理接口
		 */
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			/**
			 * 获取AOP配置的目标类
			 */
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			/**
			 * 如果配置的AOP目标类是接口，则使用JDK动态代理机制来生成AOP代理
			 */
			//todo 如果需要代理的类是接口则使用jdk代理
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			/**
			 * 如果AOP配置的目标类不是接口，则使用CGLIB的方式来生成AOP代理
			 */
			//todo 使用cglib代理方式
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * 判断AOP是否只配置了SpringProxy代理接口或者没有配置任何代理接口
	 *
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		/**
		 * 获取AOP配置的所有AOP代理接口
		 */
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
