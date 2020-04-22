//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.android1;

import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class AndroidSerializationInstantiator<T> implements ObjectInstantiator<T> {
    private final Class<T> type;
    private final ObjectStreamClass objectStreamClass;
    private final Method newInstanceMethod;

    public AndroidSerializationInstantiator(Class<T> type) {
        this.type = type;
        this.newInstanceMethod = getNewInstanceMethod();
        Method m = null;

        try {
            m = ObjectStreamClass.class.getMethod("lookupAny", Class.class);
        } catch (NoSuchMethodException var6) {
            throw new ObjenesisException(var6);
        }

        try {
            this.objectStreamClass = (ObjectStreamClass)m.invoke((Object)null, type);
        } catch (IllegalAccessException var4) {
            throw new ObjenesisException(var4);
        } catch (InvocationTargetException var5) {
            throw new ObjenesisException(var5);
        }
    }

    public T newInstance() {
        try {
            return this.type.cast(this.newInstanceMethod.invoke(this.objectStreamClass, this.type));
        } catch (IllegalAccessException var2) {
            throw new ObjenesisException(var2);
        } catch (IllegalArgumentException var3) {
            throw new ObjenesisException(var3);
        } catch (InvocationTargetException var4) {
            throw new ObjenesisException(var4);
        }
    }

    private static Method getNewInstanceMethod() {
        try {
            Method newInstanceMethod = ObjectStreamClass.class.getDeclaredMethod("newInstance", Class.class);
            newInstanceMethod.setAccessible(true);
            return newInstanceMethod;
        } catch (RuntimeException var1) {
            throw new ObjenesisException(var1);
        } catch (NoSuchMethodException var2) {
            throw new ObjenesisException(var2);
        }
    }
}
