package com.design.pattern.proxy.dynamic;

import com.design.pattern.proxy.statics.Person;
import sun.misc.ProxyGenerator;

import java.io.FileOutputStream;

public class JDKProxyTest {

    public static void main(String[] args) {
        try {
            Person person = (Person)new JDKMeipo().getInstance(new Customer());
            person.findLove();


            byte[] bytes = ProxyGenerator.generateProxyClass("$Proxy0",new Class[]{Person.class});
            FileOutputStream os = new FileOutputStream("/Users/quyixiao/project/my_spring/src/main/test/java/com/design/pattern/proxy/dynamic/$Proxy0.class");
            os.write(bytes);
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
