//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.perc;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class PercSerializationInstantiator<T> implements ObjectInstantiator<T> {
    private Object[] typeArgs;
    private final Method newInstanceMethod;

    public PercSerializationInstantiator(Class<T> type) {
        Class unserializableType;
        for(unserializableType = type; Serializable.class.isAssignableFrom(unserializableType); unserializableType = unserializableType.getSuperclass()) {
        }

        try {
            Class<?> percMethodClass = Class.forName("COM.newmonics.PercClassLoader.Method");
            this.newInstanceMethod = ObjectInputStream.class.getDeclaredMethod("noArgConstruct", Class.class, Object.class, percMethodClass);
            this.newInstanceMethod.setAccessible(true);
            Class<?> percClassClass = Class.forName("COM.newmonics.PercClassLoader.PercClass");
            Method getPercClassMethod = percClassClass.getDeclaredMethod("getPercClass", Class.class);
            Object someObject = getPercClassMethod.invoke((Object)null, unserializableType);
            Method findMethodMethod = someObject.getClass().getDeclaredMethod("findMethod", String.class);
            Object percMethod = findMethodMethod.invoke(someObject, "<init>()V");
            this.typeArgs = new Object[]{unserializableType, type, percMethod};
        } catch (ClassNotFoundException var9) {
            throw new ObjenesisException(var9);
        } catch (NoSuchMethodException var10) {
            throw new ObjenesisException(var10);
        } catch (InvocationTargetException var11) {
            throw new ObjenesisException(var11);
        } catch (IllegalAccessException var12) {
            throw new ObjenesisException(var12);
        }
    }

    public T newInstance() {
        try {
            return (T)this.newInstanceMethod.invoke((Object)null, this.typeArgs);
        } catch (IllegalAccessException var2) {
            throw new ObjenesisException(var2);
        } catch (InvocationTargetException var3) {
            throw new ObjenesisException(var3);
        }
    }
}
