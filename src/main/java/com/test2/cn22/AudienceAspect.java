package com.test2.cn22;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AudienceAspect {



	//定义切点
	@Pointcut("execution(* com.test2.cn22.impl.ShowServiceImpl.show(..))")
	public void performance() {
		//该方法的内容不重要，该方法的本身只是个标识，供@Pointcut注解依附
	}

	@Around("performance()")
	public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
		System.out.println("around before advice===========");
		Object retVal = pjp.proceed(new Object[]{"around"});
		System.out.println("around after advice===========");
		return retVal;
	}


}
