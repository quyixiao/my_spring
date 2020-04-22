package com.test1;

import com.test.LogUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Service
public class ProviderImpl implements Provider<Knife> , ApplicationContextAware {

    public static ApplicationContext applicationContext;

    @Override
    public Knife get() {
        return applicationContext.getBean(Knife.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        LogUtils.info("setApplicationContext .....................");
        this.applicationContext = applicationContext;
    }
}
