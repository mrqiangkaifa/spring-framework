/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.framework.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * Default implementation of the {@link AdvisorAdapterRegistry} interface.
 * Supports {@link org.aopalliance.intercept.MethodInterceptor},
 * {@link org.springframework.aop.MethodBeforeAdvice},
 * {@link org.springframework.aop.AfterReturningAdvice},
 * {@link org.springframework.aop.ThrowsAdvice}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {
	/**
	 * 持有通知适配器的集合
	 */
	private final List<AdvisorAdapter> adapters = new ArrayList<>(3);


	/**
	 * 构造方法，为通知适配器集合添加Spring的3种类型通知适配器
	 * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
	 */
	public DefaultAdvisorAdapterRegistry() {
		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}

	/**
	 * 将通知封装为通知器
	 * @param adviceObject
	 * @return
	 * @throws UnknownAdviceTypeException
	 */
	@Override
	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
		/**
		 * 如果通知对象是通知器类型，则不用封装
		 */
		//todo 如果需要封装的对象---是---Advisor类型就不需要处理
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
		//todo 此方法值对Advisor和Advice类型两种数据进行封装，如果不知就不能封装
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}
		/**
		 * 如果通知是方法拦截器
		 */
		//todo 如果需要封装的对象---是---MethodInterceptor类型，则使用DefaultPointcutAdvisor,不需要适配
		Advice advice = (Advice) adviceObject;
		if (advice instanceof MethodInterceptor) {
			/**
			 * 将方法拦截器类型的通知封装为默认切入点通知器
			 */
			// So well-known it doesn't even need an adapter.
			return new DefaultPointcutAdvisor(advice);
		}
		//todo 如果是Advisor的适配器那么也同样需要进行封装
		for (AdvisorAdapter adapter : this.adapters) {
			/**
			 * 检查通知适配器是否支持给定的通知
			 */
			//todo 检查是否是支持适配的类型
			// Check that it is supported.
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	/**
	 * 获取通知器的通知
	 * @param advisor the Advisor to find an interceptor for
	 * @return
	 * @throws UnknownAdviceTypeException
	 */
	@Override
	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<>(3);
		/**
		 * 通知通知器的通知
		 */
		Advice advice = advisor.getAdvice();
		/**
		 * 如果通知是方法拦截器类型，则不需要适配，直接添加到通知集合中
		 */
		if (advice instanceof MethodInterceptor) {
			interceptors.add((MethodInterceptor) advice);
		}
		/**
		 * 对通知进行适配，从适配器中获取封装好AOP编制功能的拦截器
		 */
		for (AdvisorAdapter adapter : this.adapters) {
			if (adapter.supportsAdvice(advice)) {
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
		return interceptors.toArray(new MethodInterceptor[0]);
	}

	/**
	 * 注册通知适配器
	 * @param adapter an AdvisorAdapter that understands particular Advisor or Advice types
	 */
	@Override
	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		this.adapters.add(adapter);
	}

}
