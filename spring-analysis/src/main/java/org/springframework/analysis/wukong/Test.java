package org.springframework.analysis.wukong;


import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticMessageSource;

/**
 * @program: spring
 * @description: spring测试
 * @author: wukong
 * @create: 2020-05-07 20:34
 **/
public class Test {
	public static void main(String[] args) {
		System.out.println("ceshi");
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("simpleContext.xml");
		StaticMessageSource object = (StaticMessageSource)ctx.getBean("someMessageSource");
		ctx.close();
	}


}
