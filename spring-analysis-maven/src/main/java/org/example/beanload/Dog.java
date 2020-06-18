package org.example.beanload;
/**
 * @description: Dog 
 * @projectName spring
 * @author wukong
 * @date 2020/6/8 13:04
 */
public class Dog {
	public Integer age;
	public String name;

	public void say(){
		System.out.println("my name is "+name+" . age is "+age);
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public void setName(String name) {
		this.name = name;
	}
}
