package org.example.test;

/**
 * @program: spring
 * @description: test
 * @author: wukong
 * @create: 2020-05-12 22:04
 **/
public interface IBaseService<T> {
	public T Get(T t);
}
