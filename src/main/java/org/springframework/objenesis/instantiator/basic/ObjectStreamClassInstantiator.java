//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.basic;

import java.io.ObjectStreamClass;
import java.lang.reflect.Method;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class ObjectStreamClassInstantiator<T> implements ObjectInstantiator<T> {
    private static Method newInstanceMethod;
    private final ObjectStreamClass objStreamClass;

    private static void initialize() {
        if (newInstanceMethod == null) {
            try {
                newInstanceMethod = ObjectStreamClass.class.getDeclaredMethod("newInstance");
                newInstanceMethod.setAccessible(true);
            } catch (RuntimeException var1) {
                throw new ObjenesisException(var1);
            } catch (NoSuchMethodException var2) {
                throw new ObjenesisException(var2);
            }
        }

    }

    public ObjectStreamClassInstantiator(Class<T> type) {
        initialize();
        this.objStreamClass = ObjectStreamClass.lookup(type);
    }

    public T newInstance() {
        try {
            return (T)newInstanceMethod.invoke(this.objStreamClass);
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }
}
