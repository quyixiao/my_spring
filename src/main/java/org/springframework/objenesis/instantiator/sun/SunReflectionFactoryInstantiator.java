//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.sun;

import java.lang.reflect.Constructor;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class SunReflectionFactoryInstantiator<T> implements ObjectInstantiator<T> {
    private final Constructor<T> mungedConstructor;

    public SunReflectionFactoryInstantiator(Class<T> type) {
        Constructor<Object> javaLangObjectConstructor = getJavaLangObjectConstructor();
        this.mungedConstructor = SunReflectionFactoryHelper.newConstructorForSerialization(type, javaLangObjectConstructor);
        this.mungedConstructor.setAccessible(true);
    }

    public T newInstance() {
        try {
            return this.mungedConstructor.newInstance((Object[])null);
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }

    private static Constructor<Object> getJavaLangObjectConstructor() {
        try {
            return Object.class.getConstructor((Class[])null);
        } catch (NoSuchMethodException var1) {
            throw new ObjenesisException(var1);
        }
    }
}
