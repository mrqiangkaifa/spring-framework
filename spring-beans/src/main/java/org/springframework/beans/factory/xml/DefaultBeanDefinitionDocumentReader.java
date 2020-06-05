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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private XmlReaderContext readerContext;

	@Nullable
	private BeanDefinitionParserDelegate delegate;


	/**
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		/**
		 * 获得XML描述符
		 */
		this.readerContext = readerContext;
		doRegisterBeanDefinitions(doc.getDocumentElement());
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * 根据Spring DTD对Bean的定义规则解析Bean定义Document对象
	 *
	 * Register each bean definition within the given root {@code <beans/>} element.
	 */
	@SuppressWarnings("deprecation")  // for Environment.acceptsProfiles(String...)
	protected void doRegisterBeanDefinitions(Element root) {
		/**
		 * 具体的解析过程由BeanDefinitionParserDelegate实现，
		 * BeanDefinitionParserDelegate中定义了Spring Bean定义XML文件的各种元素
		 */
		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		BeanDefinitionParserDelegate parent = this.delegate;
		this.delegate = createDelegate(getReaderContext(), root, parent);
		/**
		 * Bean定义的Document对象使用了Spring默认的XML命名空间
		 */
			if (this.delegate.isDefaultNamespace(root)) {
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				// We cannot use Profiles.of(...) since profile expressions are not supported
				// in XML config. See SPR-12458 for details.
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}
		/**
		 * 在解析Bean定义之前，进行自定义的解析，增强解析过程的可扩展性
		 */
		preProcessXml(root);
		/**
		 * 从Document的根元素开始进行Bean定义的Document对象
		 */
		parseBeanDefinitions(root, this.delegate);
		/**
		 * 在解析Bean定义之后，进行自定义的解析，增加解析过程的可扩展性
		 */
		postProcessXml(root);

		this.delegate = parent;
	}

	/**
	 * 创建BeanDefinitionParserDelegate，用于完成真正的解析过程
	 * @param readerContext
	 * @param root
	 * @param parentDelegate
	 * @return
	 */
	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {

		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		/**
		 * BeanDefinitionParserDelegate初始化Document根元素
		 */
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * 使用Spring的Bean规则从Document的根元素开始进行Bean定义的Document对象
	 *
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		/**
		 * Bean定义的Document对象使用了Spring默认的XML命名空间
		 */
		//todo．对默认bean的处理，即获取的节点的URI中包含http://www.springframework.org/schema/beans
		if (delegate.isDefaultNamespace(root)) {
			/**
			 * 获取Bean定义的Document对象根元素的所有子节点
			 */
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				/**
				 * 获得Document节点是XML元素节点
				 */
				if (node instanceof Element) {
					Element ele = (Element) node;
					/**
					 * Bean定义的Document的元素节点使用的是Spring默认的XML命名空间
					 */
					if (delegate.isDefaultNamespace(ele)) {
						/**
						 * 使用Spring的Bean规则解析元素节点
						 */
						parseDefaultElement(ele, delegate);
					}
					else {
						/**
						 * 没有使用Spring默认的XML命名空间，则使用用户自定义的解析规则解析元素节点
						 */
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {
			/**
			 * Document的根节点没有使用Spring默认的命名空间，则使用用户自定义的
			 * 解析规则解析Document根节点
			 */
			delegate.parseCustomElement(root);
		}
	}

	/**
	 * 使用Spring的Bean规则解析Document元素节点
	 * @param ele
	 * @param delegate
	 */
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {

		//todo.对import标签的处理
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			/**
			 * 如果元素节点是<Import>导入元素，进行导入解析
			 */
			importBeanDefinitionResource(ele);
		}
		//todo.对alias标签的处理
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			/**
			 * 如果元素节点是<Alias>别名元素，进行别名解析
			 */
			processAliasRegistration(ele);
		}
		//todo.对bean标签的处理
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			/**
			 * 元素节点既不是导入元素，也不是别名元素，即普通的<Bean>元素，
			 * 按照Spring的Bean规则解析元素
			 */
			processBeanDefinition(ele, delegate);
		}
		//todo.对beans标签的处理
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			/**
			 * Beans标签
			 */
			// recurse
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * 解析<Import>导入元素，从给定的导入路径加载Bean定义资源到Spring IoC容器中
	 *
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 */
	protected void importBeanDefinitionResource(Element ele) {
		/**
		 * 获取给定的导入元素的location属性
		 */
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}
		/**
		 * 使用系统变量值解析location属性值 解析系统属性，格式如：“${user.dir}”
		 */
		// Resolve system properties: e.g. "${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);
		/**
		 * 标识给定的导入元素的location是否是绝对路径
		 * 判断Location是决定URI还是URL
		 */
		// Discover whether the location is an absolute or relative URI
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}
		/**
		 * 给定的导入元素的location是绝对路径
		 * 如果是绝对路径，则直接根据地址解析对应的配置
		 */
		// Absolute or relative?

		if (absoluteLocation) {
			try {
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {
			/**
			 * 给定的导入元素的location是相对路径
			 */
			//todo．如果是相时地址则根据相对地址计算出绝对地址
			// No URL -> considering resource location as relative to the current file.
			try {
				int importCount;
				/**
				 * 将给定导入元素的location封装为相对路径资源
				 */
				//todo.前面提到Resource存在多个子类，而每个子类的createRelative方式实现都是不一样的，所以这里先使用子类的方法尝试
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				/**
				 * 封装的相对路径资源存在
				 */
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					/**
					 * 如果解析不成功那么使用默认的ResourcePatternResolver进行解析
					 * 封装的相对路径资源不存在
					 * 获取Spring IoC容器资源读入器的基本路径
					 */

					String baseLocation = getReaderContext().getResource().getURL().toString();
					/**
					 * 根据Spring IoC容器资源读入器的基本路径加载给定导入路径的资源
					 */
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		/**
		 * 在解析完<Import>元素之后，发送容器导入其他资源处理完成事件
		 */
		//todo.解析完成之后通知监听器进行处理
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * 解析<Alias>别名元素，为Bean向Spring IoC容器注册别名
	 *
	 * Process the given alias element, registering the alias with the registry.
	 */
	protected void processAliasRegistration(Element ele) {
		/**
		 * 获取<Alias>别名元素中name的属性值
		 */
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		/**
		 * 获取<Alias>别名元素中alias的属性值
		 */
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		/**
		 * <alias>别名元素的name属性值为空
		 */
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		/**
		 * <alias>别名元素的alias属性值为空
		 */
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				/**
				 * 向容器的资源读入器注册别名
				 */
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			/**
			 * 在解析完<Alias>元素之后，发送容器别名处理完成事件
			 */
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * 解析Bean定义资源Document对象的普通元素
	 *
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		/**
		 * BeanDefinitionHolder是对BeanDefinition的封装，即Bean定义的封装类
		 * 对Document对象中<Bean>元素的解析由BeanDefinitionParserDelegate实现
		 * 进行元素解断返回BeanDefinitionHolder类型的实例bdHolder,经过这个方法之后,bdHolder对象已经包含了配置文件中的各种属性,比class, name, id, alias
		 */
		//todo.进行元素解断返回BeanDefinitionHolder类型的实例bdHolder,经过这个方法之后,bdHolder对象已经包含了配置文件中的各种属性,比class, name, id, alias
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			/**
			 * 当返回的bdHolder不为空的情况下若存在默认存在的子节点下再有自定义属件，还需要再次对自定义标签进行解析
			 * <bean   id="test"  class="">
			 *   <mybean:user  username="aaa"/>
			 * </bean>
			 */
			//todo.当返回的bdHolder不为空的情况下若存在默认存在的子节点下再有自定义属件，还需要再次对自定义标签进行解析
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				/**
				 * 向Spring IoC容器注册解析得到的Bean定义，这是Bean定义向IoC容器注册的入口
				 */
				//todo.解析完成之后,需要对解析dHolder进行注册,同样,注册操作委托给了BeanDefinitionReaderutils的registerBeanDefinition方法
				// Register the final decorated instance.
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			/**
			 * 在完成向Spring IoC容器注册解析得到的Bean定义之后，发送注册事件
			 */
			//todo.最后发出响应时间，通知相关的监听器，这个bean已经加载完成了
			// Send registration event.
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
