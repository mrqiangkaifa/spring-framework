package org.example.test;

/**
 * @program: spring
 * @description: test
 * @author: wukong
 * @create: 2020-05-12 22:12
 **/
public class AServiceImpl implements IARelService {
	private Person person;
	@Override
	public Person Get(Person o) {
		return o;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}
}
