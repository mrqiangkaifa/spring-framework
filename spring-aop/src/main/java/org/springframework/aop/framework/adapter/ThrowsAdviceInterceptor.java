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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AfterAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Interceptor to wrap an after-throwing advice.
 *
 * <p>The signatures on handler methods on the {@code ThrowsAdvice}
 * implementation method argument must be of the form:<br>
 *
 * {@code void afterThrowing([Method, args, target], ThrowableSubclass);}
 *
 * <p>Only the last argument is required.
 *
 * <p>Some examples of valid methods would be:
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
 *
 * <p>This is a framework class that need not be used directly by Spring users.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see MethodBeforeAdviceInterceptor
 * @see AfterReturningAdviceInterceptor
 */
public class ThrowsAdviceInterceptor implements MethodInterceptor, AfterAdvice {

	private static final String AFTER_THROWING = "afterThrowing";

	private static final Log logger = LogFactory.getLog(ThrowsAdviceInterceptor.class);


	private final Object throwsAdvice;

	/**
	 * key为类，value为method的异常处理map
	 * Methods on throws advice, keyed by exception class. */
	private final Map<Class<?>, Method> exceptionHandlerMap = new HashMap<>();


	/**
	 * 异常通知拦截器构造方法
	 * Create a new ThrowsAdviceInterceptor for the given ThrowsAdvice.
	 * @param throwsAdvice the advice object that defines the exception handler methods
	 * (usually a {@link org.springframework.aop.ThrowsAdvice} implementation)
	 */
	public ThrowsAdviceInterceptor(Object throwsAdvice) {
		Assert.notNull(throwsAdvice, "Advice must not be null");
		/**
		 * 初始化异常通知
		 */
		this.throwsAdvice = throwsAdvice;
		/**
		 * 获取异常通知的方法
		 */
		Method[] methods = throwsAdvice.getClass().getMethods();
		/**
		 * 遍历所有配置异常通知的方法
		 */
		for (Method method : methods) {
			/**
			 * 如果方法名为"afterThrowing"，且方法参数为1或4，且方法参数的父类为
			 * Throwable，说明异常通知中有异常处理器
			 */
			if (method.getName().equals(AFTER_THROWING) &&
					(method.getParameterCount() == 1 || method.getParameterCount() == 4)) {
				Class<?> throwableParam = method.getParameterTypes()[method.getParameterCount() - 1];
				if (Throwable.class.isAssignableFrom(throwableParam)) {
					/**
					 * 添加异常处理器
					 */
					// An exception handler to register...
					this.exceptionHandlerMap.put(throwableParam, method);
					if (logger.isDebugEnabled()) {
						logger.debug("Found exception handler method on throws advice: " + method);
					}
				}
			}
		}
		/**
		 * 没有配置异常处理器
		 */
		if (this.exceptionHandlerMap.isEmpty()) {
			throw new IllegalArgumentException(
					"At least one handler method must be found in class [" + throwsAdvice.getClass() + "]");
		}
	}


	/**
	 * 获取异常处理器数目
	 * Return the number of handler methods in this advice.
	 */
	public int getHandlerMethodCount() {
		return this.exceptionHandlerMap.size();
	}

	/**
	 * 异常通知的回调方法
	 * @param mi
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		/**
		 * 把目标对象方法调用放入try/catch中
		 */
		try {
			return mi.proceed();
		}
		catch (Throwable ex) {
			/**
			 * 在catch中触发异常通知的回调
			 */
			Method handlerMethod = getExceptionHandler(ex);
			if (handlerMethod != null) {
				/**
				 * 使用异常处理器处理异常
				 */
				invokeHandlerMethod(mi, ex, handlerMethod);
			}
			/**
			 * 将异常向上抛出
			 */
			throw ex;
		}
	}

	/**
	 * 获取异常处理器
	 * Determine the exception handle method for the given exception.
	 * @param exception the exception thrown
	 * @return a handler for the given exception type, or {@code null} if none found
	 */
	@Nullable
	private Method getExceptionHandler(Throwable exception) {
		/**
		 * 获取给定异常类
		 */
		Class<?> exceptionClass = exception.getClass();
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to find handler for exception of type [" + exceptionClass.getName() + "]");
		}
		/**
		 * 从异常处理器map中获取给定异常类的处理器
		 */
		Method handler = this.exceptionHandlerMap.get(exceptionClass);
		/**
		 * 如果异常处理器为null，且异常类不是Throwable
		 */
		while (handler == null && exceptionClass != Throwable.class) {
			/**
			 * 获取异常类的基类
			 */
			exceptionClass = exceptionClass.getSuperclass();
			/**
			 * 获取基类的异常处理器
			 */
			handler = this.exceptionHandlerMap.get(exceptionClass);
		}
		if (handler != null && logger.isTraceEnabled()) {
			logger.trace("Found handler for exception of type [" + exceptionClass.getName() + "]: " + handler);
		}
		return handler;
	}

	/**
	 * 异常处理器处理异常
	 * @param mi
	 * @param ex
	 * @param method
	 * @throws Throwable
	 */
	private void invokeHandlerMethod(MethodInvocation mi, Throwable ex, Method method) throws Throwable {
		Object[] handlerArgs;
		/**
		 * 如果方法只有一个参数
		 */
		if (method.getParameterCount() == 1) {
			handlerArgs = new Object[] {ex};
		}
		/**
		 * 获取方法的参数列表
		 */
		else {
			handlerArgs = new Object[] {mi.getMethod(), mi.getArguments(), mi.getThis(), ex};
		}
		try {
			/**
			 * 使用JDK反射机制，调用异常通知的异常处理方法
			 */
			method.invoke(this.throwsAdvice, handlerArgs);
		}
		catch (InvocationTargetException targetEx) {
			throw targetEx.getTargetException();
		}
	}

}
