/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;

/**
 * {@link BeanDefinitionParser} for the {@code aspectj-autoproxy} tag,
 * enabling the automatic application of @AspectJ-style aspects found in
 * the {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
class AspectJAutoProxyBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		/**
		 * proxy-target-class：SpringAop部分使用JDK动态代理或者CGLIB来为目标对象创建代理。（建议使用JDK的动态代理），如果==被代理的目标对象实现了至少一个接口==，则==会使用JDK动态代理==。所有该目标类型实现的接口都将被代理。==若该目标对象没有实现任何接口，则创建一个CGLIB代理==。如果你希望强制使用CGLIB代理，需要考虑一下两个问题。
		 *
		 * 无法通知（advise）Final方法，因为它们不能被复写
		 * 需要将CGLIB二进制发行包放在classpath下面。
		 * 与之相较，JDK本身就提供了动态代理，强制使用CGLIB代理需要将<aop:config>的proxy-target-class属性设置为true
		 * <aop:config proxy-target-class="true">...</aop:config>
		 * 当需要使用CGLIB代理和@AspectJ自动代理支持，可以按照一下方式设置<aop:aspectj-autoproxy>的proxy-target-class属性：
		 * <aop:aspectj-autoproxy proxy-target-class="true">
		 * JDK动态代理：其代理对象必须是某个接口的实现，它是通过在运行期间建一个接口的实现类来完成对目标对象的代理
		 * CGLIB代理：实现原理类似于JDK动态代理，只是它在运行期间生成的代理对象是针对目标类扩展的子类。CGLIB是高级的代码生成包，底层是靠ASM（开源的Java字节码编辑类库）。操作字节码实现的，性能比JDk强。
		 * expose-proxy：有时候目标对象内部的自我调用将无法实现切面中的增强，如下示例：
		 *public interface AService {
		 *     void a();
		 *
		 *     void b();
		 * }
		 *
		 * @Service
		 * public class AServiceImpl implements AService{
		 *     @Override
		 *     @Transactional(propagation = Propagation.REQUIRED)
		 *     public void a() {
		 *         this.b();
		 *     }
		 *
		 *     @Override
		 *     @Transactional(propagation = Propagation.REQUIRED)
		 *     public void b() {
		 *
		 *     }
		 * }
		 * 此处的this指向目标对象，因此调用this.b()将不会执行b事务切面，即不会执行事务增强，因此b方法的事务定义“ @Transactional(propagation = Propagation.REQUIRED)”将不会实施，为了解决这个问题，我们可以这么做：
		 *<aop:aspectj-autoproxy expose-proxy="true"/>
		 *  然后将上面代码中的this.b()修改为“((AService)AopContext.currentProxy()).b()”即可。通过以上的修改就可以完成a对b方法的同步增强。
		 */
		//todo 注册AnnotationAwareAspectJAutoProxyCreator
		AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
		//todo 对注解中子类的处理
		extendBeanDefinition(element, parserContext);
		return null;
	}

	private void extendBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinition beanDef =
				parserContext.getRegistry().getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		if (element.hasChildNodes()) {
			addIncludePatterns(element, parserContext, beanDef);
		}
	}

	private void addIncludePatterns(Element element, ParserContext parserContext, BeanDefinition beanDef) {
		ManagedList<TypedStringValue> includePatterns = new ManagedList<>();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element includeElement = (Element) node;
				TypedStringValue valueHolder = new TypedStringValue(includeElement.getAttribute("name"));
				valueHolder.setSource(parserContext.extractSource(includeElement));
				includePatterns.add(valueHolder);
			}
		}
		if (!includePatterns.isEmpty()) {
			includePatterns.setSource(parserContext.extractSource(element));
			beanDef.getPropertyValues().add("includePatterns", includePatterns);
		}
	}

}
