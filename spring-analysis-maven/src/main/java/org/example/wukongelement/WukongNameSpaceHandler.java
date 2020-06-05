package org.example.wukongelement;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @description: 命名空间处理器
 * @projectName spring
 * @author wukong
 * @date 2020/6/4 13:31
 */
public class WukongNameSpaceHandler extends NamespaceHandlerSupport {
	@Override
	public void init() {
		registerBeanDefinitionParser("wukong", BeanUtils.instantiateClass(WukongBeanDefinitionParser.class));
	}
}
