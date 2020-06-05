package org.example.test;

import org.springframework.beans.factory.FactoryBean;

/**
 * @description: 测试工厂
 * @projectName spring
 * @author wukong
 * @date 2020/6/4 17:09
 */
public class TestFactoryBean implements FactoryBean<Car> {
	private  String carInfo ;
	@Override
	public Car getObject() throws Exception {
		Car car =  new  Car () ;
		String []  infos =  carInfo .split ( "," ) ;
		car.setBrand ( infos [ 0 ]) ;
		car.setMaxSpeed ( Integer. valueOf ( infos [ 1 ])) ;
		car.setPrice ( Double. valueOf ( infos [ 2 ])) ;
		return  car;
	}

	@Override
	public Class<Car> getObjectType() {
		return  Car. class ;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public String getCarInfo() {
		return carInfo;
	}
	// 接受逗号分割符设置属性信息
	public void setCarInfo(String carInfo) {
		this.carInfo = carInfo;
	}
}
