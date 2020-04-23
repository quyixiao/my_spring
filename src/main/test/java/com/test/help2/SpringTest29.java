package com.test.help2;

import com.test2.cn28.Thinker;
import com.test2.cn29.Contestant;
import com.test2.cn29.XiaoTou;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.ConcurrencyFailureException;

public class SpringTest29 {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:*/spring29.xml");
        XiaoTou xiaoTou = context.getBean(XiaoTou.class);
        Contestant contestant = (Contestant)xiaoTou;
        contestant.receiveAward();
        xiaoTou.chang();
    }
}
