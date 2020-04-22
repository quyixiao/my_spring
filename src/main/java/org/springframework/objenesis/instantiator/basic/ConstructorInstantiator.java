//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.basic;

import java.lang.reflect.Constructor;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class ConstructorInstantiator<T> implements ObjectInstantiator<T> {
    protected Constructor<T> constructor;

    public ConstructorInstantiator(Class<T> type) {
        try {
            this.constructor = type.getDeclaredConstructor((Class[])null);
        } catch (Exception var3) {
            throw new ObjenesisException(var3);
        }
    }

    public T newInstance() {
        try {
            return this.constructor.newInstance((Object[])null);
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }
}
