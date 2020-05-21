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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {
	/**
	 * 获取通知
	 * @param config the AOP configuration in the form of an Advised object
	 * @param method the proxied method
	 * @param targetClass the target class (may be {@code null} to indicate a proxy without
	 * target object, in which case the method's declaring class is the next best option)
	 * @return
	 */
	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		/**
		 * 获取通知适配器注册单态模式对象
		 */
		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		Advisor[] advisors = config.getAdvisors();
		/**
		 * 根据AOP中配置的通知器创建一个保持获取到通知的集合
		 */
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		/**
		 * 判断目标类中是否引入了配置的通知
		 */
		Boolean hasIntroductions = null;
		/**
		 * 遍历AOP配置的通知
		 */
		for (Advisor advisor : advisors) {
			/**
			 * 如果通知器的类型是切入点通知器
			 */
			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				/**
				 * 如果AOP配置对通知已经过滤，即只包含符合条件的通知器，或者
				 * 获取当前切入点的类过滤器匹配目标类
				 */
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					/**
					 * 获取当前通知器切入点的方法匹配器
					 */
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;

					if (mm instanceof IntroductionAwareMethodMatcher) {
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					}
					else {
						match = mm.matches(method, actualClass);
					}
					/**
					 * 如果目标类的方法匹配切入点
					 */
					if (match) {
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						/**
						 * 如果方法匹配器是运行时动态匹配的
						 */
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							for (MethodInterceptor interceptor : interceptors) {
								/**
								 * 将通知器中的方法拦截器和方法匹配器封装后添加到返回的通知器集合中
								 */
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						/**
						 * 如果方法匹配器是静态的，则将方法拦截器直接添加到返回的通知器集合中
						 */
						else {
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			/**
			 * 如果通知器类型是引入通知器
			 */
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				/**
				 * 如果AOP通知配置是预过滤的，或者目标类符合当前通知器的类过滤器
				 */
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					/**
					 * 获取通知器中所有的方法拦截器，即通知
					 */
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					/**
					 * 将通知添加到要返回的通知集合中
					 */
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			/**
			 * 如果通知类型既不是切入点通知器，又不是引入通知器
			 */
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				/**
				 * 直接将通知添加到要返回的通知集合中
				 */
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		return interceptorList;
	}

	/**
	 * 检查目标类和AOP通知配置是否匹配AOP引入规则
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		/**
		 * 遍历所有的通知器
		 */
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				/**
				 * 如果通知器是引入通知器
				 */
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				/**
				 * 使用当前通知器的类过滤器匹配目标类
				 */
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
