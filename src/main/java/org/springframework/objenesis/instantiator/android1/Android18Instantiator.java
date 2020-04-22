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

public class Android18Instantiator<T> implements ObjectInstantiator<T> {
    private final Class<T> type;
    private final Method newInstanceMethod;
    private final Long objectConstructorId;

    public Android18Instantiator(Class<T> type) {
        this.type = type;
        this.newInstanceMethod = getNewInstanceMethod();
        this.objectConstructorId = findConstructorIdForJavaLangObjectConstructor();
    }

    public T newInstance() {
        try {
            return this.type.cast(this.newInstanceMethod.invoke((Object)null, this.type, this.objectConstructorId));
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }

    private static Method getNewInstanceMethod() {
        try {
            Method newInstanceMethod = ObjectStreamClass.class.getDeclaredMethod("newInstance", Class.class, Long.TYPE);
            newInstanceMethod.setAccessible(true);
            return newInstanceMethod;
        } catch (RuntimeException var1) {
            throw new ObjenesisException(var1);
        } catch (NoSuchMethodException var2) {
            throw new ObjenesisException(var2);
        }
    }

    private static Long findConstructorIdForJavaLangObjectConstructor() {
        try {
            Method newInstanceMethod = ObjectStreamClass.class.getDeclaredMethod("getConstructorId", Class.class);
            newInstanceMethod.setAccessible(true);
            return (Long)newInstanceMethod.invoke((Object)null, Object.class);
        } catch (RuntimeException var1) {
            throw new ObjenesisException(var1);
        } catch (NoSuchMethodException var2) {
            throw new ObjenesisException(var2);
        } catch (IllegalAccessException var3) {
            throw new ObjenesisException(var3);
        } catch (InvocationTargetException var4) {
            throw new ObjenesisException(var4);
        }
    }
}
