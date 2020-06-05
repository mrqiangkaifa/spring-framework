package org.example.wukongelement;


import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * @description: 自定义标签解析器
 * @projectName spring
 * @author wukong
 * @date 2020/6/4 13:19
 */
public class WukongBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
	//todo element对应的标签
	@Override
	protected Class<?> getBeanClass(Element element) {
		return User.class;
	}
	//todo 从element中解析并提取对应的元素
	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String id = element.getAttribute("id");
		String name = element.getAttribute("name");
		String email = element.getAttribute("email");
		//todo 将解析出来的属性放到BeanDefinitionBulder中，待到完成所有bean的解析后统一注册到beanFactory中
		builder.addPropertyValue("id",id);
		builder.addPropertyValue("name",name);
		builder.addPropertyValue("email",email);

	}
}
