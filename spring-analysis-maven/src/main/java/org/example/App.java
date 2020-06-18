package org.example;

import org.example.aop.UserService;
import org.example.beanload.AppBeanLoad;
import org.example.test.AServiceImpl;
import org.example.test.Car;
import org.example.test.IBaseService;
import org.example.test.Person;
import org.example.wukongelement.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		ApplicationContext context = new ClassPathXmlApplicationContext("simpleContext.xml");
		StaticMessageSource staticMessageSource = (StaticMessageSource)context.getBean("someMessageSource");
		Car testFactoryCar = (Car)context.getBean("car");
		AServiceImpl  iBaseService = (AServiceImpl)context.getBean(IBaseService.class);
		Method method = iBaseService.getClass().getDeclaredMethod("Get",Person.class);
		Person person = new Person();
		person.setName("test");
		Object o = method.invoke(iBaseService,person);
		System.out.println(o.toString());
		System.out.println(iBaseService.getPerson().getService().Get(person));
		System.out.println("ceshi");
		System.out.println(context.getApplicationName());
		//todo 自定义标签
		User testUser = (User)context.getBean("testUser");

		//todo spring简单使用
		AppBeanLoad.main(args);

		//todo aop
		UserService userServiceProxy = (UserService)context.getBean("userServiceProxy");
		userServiceProxy.addUser();
		//todo aopcofig
		UserService userServiceAopConfig = (UserService)context.getBean("userServiceAopConfig");
		userServiceAopConfig.addUser();
    }
}
