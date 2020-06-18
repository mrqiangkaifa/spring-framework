package org.springframework.analysis.wukong.aop.aopconfig;

import org.springframework.analysis.wukong.aop.UserService;

/**
 * @description: aopconfig
 * @projectName spring
 * @author wukong
 * @date 2020/6/10 15:26
 */
public class UserServiceAopConfigImpl implements UserService {
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