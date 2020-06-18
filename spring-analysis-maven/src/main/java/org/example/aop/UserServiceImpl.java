package org.example.aop;

import org.example.aop.UserService;

/**
 * @description: UserServiceImpl 
 * @projectName spring
 * @author wukong
 * @date 2020/6/10 15:02
 */
public class UserServiceImpl implements UserService {
	@Override
	public void addUser() {
		System.out.println("addUser()");
	}

	@Override
	public void updateUser() {
		System.out.println("updateUser()");
	}

	@Override
	public void deleteUser() {
		System.out.println("deleteUser()");
	}
}
