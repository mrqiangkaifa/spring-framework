package org.example;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticMessageSource;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
		ApplicationContext context = new ClassPathXmlApplicationContext("simpleContext.xml");
		StaticMessageSource staticMessageSource = (StaticMessageSource)context.getBean("someMessageSource");
		System.out.println("ceshi");
		System.out.println(context.getApplicationName());
    }
}
