package org.example.test;

/**
 * @program: spring
 * @description: TEST
 * @author: wukong
 * @create: 2020-05-12 22:13
 **/
public class Person {
	private String name ;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
