//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.strategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class SingleInstantiatorStrategy implements InstantiatorStrategy {
    private Constructor<?> constructor;

    public <T extends ObjectInstantiator<?>> SingleInstantiatorStrategy(Class<T> instantiator) {
        try {
            this.constructor = instantiator.getConstructor(Class.class);
        } catch (NoSuchMethodException var3) {
            throw new ObjenesisException(var3);
        }
    }

    public <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> type) {
        try {
            return (ObjectInstantiator)this.constructor.newInstance(type);
        } catch (InstantiationException var3) {
            throw new ObjenesisException(var3);
        } catch (IllegalAccessException var4) {
            throw new ObjenesisException(var4);
        } catch (InvocationTargetException var5) {
            throw new ObjenesisException(var5);
        }
    }
}
