package org.example.beanload;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Field;

/**
 * @description: app
 * @projectName spring
 * @author wukong
 * @date 2020/6/8 13:02
 */
public class AppBeanLoad {
	public static void main(String[] args) {
		new Dog().say();

		//todo 准备生成dog的定义
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(Dog.class);
		//todo 设置它的属性
		builder.addPropertyValue("name","john");
		builder.addPropertyValue("age",1);

		//todo 生成beanFactory
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//todo 添加dog定义
		beanFactory.registerBeanDefinition("dog",builder.getBeanDefinition());

		//todo 获取dog
		Dog dog = (Dog) beanFactory.getBean("dog");
		dog.say();
		//todo 反射生成对象,spring创建对象的简化版
		try {
			//todo 获取Dog类
			Class dogClass = Class.forName("org.example.beanload.Dog");
			//todo 通过类生成对象
			Dog dog1 = (Dog) dogClass.newInstance();
			//todo 获取特定的属性
			Field field = dogClass.getField("name");
			//todo 填充属性
			field.set(dog1,"landa");
			dog1.say();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
