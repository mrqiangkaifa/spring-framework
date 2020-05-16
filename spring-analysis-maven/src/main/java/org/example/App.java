package org.example;

import org.example.test.AServiceImpl;
import org.example.test.IBaseService;
import org.example.test.Person;
import org.springframework.context.ApplicationContext;
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
		ApplicationContext context = new FileSystemXmlApplicationContext("D:\\work_space\\spring\\spring-framework\\spring-analysis-maven\\src\\main\\resources\\simpleContext.xml");
		StaticMessageSource staticMessageSource = (StaticMessageSource)context.getBean("someMessageSource");
		AServiceImpl  iBaseService = (AServiceImpl)context.getBean(IBaseService.class);
		Method method = iBaseService.getClass().getDeclaredMethod("Get",Person.class);
		Person person = new Person();
		person.setName("test");
		Object o = method.invoke(iBaseService,person);
		System.out.println(o.toString());
		System.out.println("ceshi");
		System.out.println(context.getApplicationName());
    }
}
